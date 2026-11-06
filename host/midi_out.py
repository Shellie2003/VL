"""MIDI output of Vôlavo Hôte: forwards the phone's notes to a DAW (FL Studio, Ableton…).

Windows has no built-in way for an application to create a MIDI port, so:
  1. teVirtualMIDI (installed with the free "loopMIDI" by Tobias Erichsen): a port named
     "Vôlavo MIDI" is created automatically — nothing to configure.
  2. python-rtmidi: sends to an existing loopMIDI / "Volavo" port.
  3. macOS / Linux: python-rtmidi creates a virtual "Vôlavo MIDI" port natively.
"""

import ctypes
import sys

PORT_NAME = "Volavo MIDI"  # ASCII: some DAWs mangle accented port names


def split_messages(data):
    """Splits a byte string of complete MIDI channel messages into individual messages."""
    out = []
    i = 0
    n = len(data)
    while i < n:
        status = data[i]
        if status < 0x80:  # stray data byte: skip
            i += 1
            continue
        kind = status & 0xF0
        if kind in (0xC0, 0xD0):
            length = 2
        elif kind == 0xF0:
            length = 1  # system real-time / common bytes are forwarded alone
        else:
            length = 3
        if i + length > n:
            break
        out.append(bytes(data[i:i + length]))
        i += length
    return out


class _TeVirtualMidi:
    """loopMIDI's driver API (teVirtualMIDI64.dll) — creates the port by itself."""

    name = "loopMIDI (port créé automatiquement)"

    def __init__(self):
        dll = None
        for candidate in ("teVirtualMIDI64.dll", "teVirtualMIDI.dll"):
            try:
                dll = ctypes.WinDLL(candidate, use_last_error=True)
                break
            except OSError:
                continue
        if dll is None:
            raise OSError("loopMIDI (teVirtualMIDI) n'est pas installé")
        create = dll.virtualMIDICreatePortEx2
        create.restype = ctypes.c_void_p
        create.argtypes = (ctypes.c_wchar_p, ctypes.c_void_p, ctypes.c_void_p,
                           ctypes.c_uint32, ctypes.c_uint32)
        self._send_fn = dll.virtualMIDISendData
        self._send_fn.restype = ctypes.c_bool
        self._send_fn.argtypes = (ctypes.c_void_p, ctypes.c_char_p, ctypes.c_uint32)
        self._close_fn = dll.virtualMIDIClosePort
        self._close_fn.argtypes = (ctypes.c_void_p,)
        # TE_VM_FLAGS_PARSE_RX (1) | TE_VM_FLAGS_INSTANTIATE_BOTH (12)
        self._port = create(PORT_NAME, None, None, 65535, 1 | 12)
        if not self._port:
            raise OSError("création du port MIDI refusée (err %d)" % ctypes.get_last_error())

    def send(self, msg):
        self._send_fn(self._port, msg, len(msg))

    def close(self):
        if self._port:
            self._close_fn(self._port)
            self._port = None


class _RtMidi:
    def __init__(self):
        import rtmidi
        self._out = rtmidi.MidiOut()
        ports = self._out.get_ports()
        if sys.platform.startswith("win"):
            wanted = [i for i, p in enumerate(ports)
                      if any(k in p.lower() for k in ("volavo", "vôlavo", "loopmidi"))]
            if not wanted:
                raise OSError("aucun port loopMIDI trouvé (installez loopMIDI)")
            self._out.open_port(wanted[0])
            self.name = "port « %s »" % ports[wanted[0]]
        else:
            self._out.open_virtual_port(PORT_NAME)
            self.name = "port virtuel « %s »" % PORT_NAME

    def send(self, msg):
        self._out.send_message(list(msg))

    def close(self):
        self._out.close_port()


class MidiOut:
    """Opened lazily on the first MIDI packet; never raises in the hot path."""

    def __init__(self, log=print):
        self._log = log
        self._backend = None
        self.error = None
        self._tried = False

    @property
    def description(self):
        if self._backend is not None:
            return self._backend.name
        return self.error or "inactif"

    def _open(self):
        self._tried = True
        errors = []
        candidates = [_TeVirtualMidi, _RtMidi] if sys.platform.startswith("win") else [_RtMidi]
        for cls in candidates:
            try:
                self._backend = cls()
                self.error = None
                self._log("MIDI : %s" % self._backend.name)
                return
            except Exception as exc:  # noqa: BLE001 — try the next back-end
                errors.append(str(exc))
        self.error = "Sortie MIDI indisponible : installez loopMIDI (gratuit)" \
            if sys.platform.startswith("win") else "Sortie MIDI indisponible : pip install python-rtmidi"
        self._log("%s (%s)" % (self.error, "; ".join(errors)))

    def send(self, data):
        if self._backend is None:
            if self._tried:
                return
            self._open()
            if self._backend is None:
                return
        for msg in split_messages(data):
            try:
                self._backend.send(msg)
            except Exception as exc:  # noqa: BLE001
                self._log("MIDI : envoi impossible (%s)" % exc)
                return

    def all_notes_off(self):
        """Panic: sustain off + all notes off on the 16 channels."""
        if self._backend is None:
            return
        for ch in range(16):
            for msg in (bytes([0xB0 | ch, 64, 0]), bytes([0xB0 | ch, 123, 0]), bytes([0xE0 | ch, 0, 0x40])):
                try:
                    self._backend.send(msg)
                except Exception:  # noqa: BLE001
                    return

    def close(self):
        if self._backend is not None:
            try:
                self.all_notes_off()
                self._backend.close()
            except Exception:  # noqa: BLE001
                pass
            self._backend = None
