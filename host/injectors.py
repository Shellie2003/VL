"""Input injection back-ends for the Vôlavo host.

Order of preference:
  * Windows : native SendInput through ctypes (no dependency, lowest latency)
  * Linux   : /dev/uinput through python-evdev (works on X11 *and* Wayland)
  * Others  : pynput (macOS Quartz, Linux X11)
  * dry-run : prints events, used for tests and debugging
"""

import sys

from volavo_protocol import (
    BTN_LEFT, BTN_MIDDLE, BTN_RIGHT, KEY_DOWN, KEY_TAP, KEY_UP,
    MOD_ALT, MOD_CTRL, MOD_META, MOD_SHIFT,
)

# --- HID usage -> logical key name -----------------------------------------
HID_NAMES = {}
for _i, _c in enumerate("abcdefghijklmnopqrstuvwxyz"):
    HID_NAMES[0x04 + _i] = _c
for _i, _c in enumerate("1234567890"):
    HID_NAMES[0x1E + _i] = _c
HID_NAMES.update({
    0x28: "enter", 0x29: "esc", 0x2A: "backspace", 0x2B: "tab", 0x2C: "space",
    0x2D: "minus", 0x2E: "equal", 0x2F: "lbracket", 0x30: "rbracket",
    0x31: "backslash", 0x33: "semicolon", 0x34: "quote", 0x35: "grave",
    0x36: "comma", 0x37: "period", 0x38: "slash",
    0x49: "insert", 0x4A: "home", 0x4B: "pageup", 0x4C: "delete", 0x4D: "end",
    0x4E: "pagedown", 0x4F: "right", 0x50: "left", 0x51: "down", 0x52: "up",
})
for _i in range(12):
    HID_NAMES[0x3A + _i] = "f%d" % (_i + 1)

MOD_NAMES = ((MOD_CTRL, "ctrl"), (MOD_SHIFT, "shift"), (MOD_ALT, "alt"), (MOD_META, "meta"))
BUTTONS = ((BTN_LEFT, "left"), (BTN_RIGHT, "right"), (BTN_MIDDLE, "middle"))
NOTCH = 120


class Injector:
    """Common logic: button diffing, key taps, scroll accumulation, safety release."""

    name = "base"
    hires_scroll = False

    def __init__(self):
        self._buttons = 0
        self._held_keys = set()
        self._held_mods = set()
        self._acc_v = 0
        self._acc_h = 0

    # --- primitives implemented by subclasses ----------------------------
    def _move(self, dx, dy):
        raise NotImplementedError

    def _button(self, name, down):
        raise NotImplementedError

    def _wheel(self, v, h):
        """v/h in 1/120 notch units when hires_scroll, else whole notches."""
        raise NotImplementedError

    def _key(self, name, down):
        raise NotImplementedError

    # --- public API --------------------------------------------------------
    def move(self, dx, dy):
        if dx or dy:
            self._move(dx, dy)

    def buttons(self, mask):
        changed = mask ^ self._buttons
        if not changed:
            return
        for bit, name in BUTTONS:
            if changed & bit:
                self._button(name, bool(mask & bit))
        self._buttons = mask

    def scroll(self, v, h):
        if self.hires_scroll:
            if v or h:
                self._wheel(v, h)
            return
        self._acc_v += v
        self._acc_h += h
        nv = int(self._acc_v / NOTCH)
        nh = int(self._acc_h / NOTCH)
        self._acc_v -= nv * NOTCH
        self._acc_h -= nh * NOTCH
        if nv or nh:
            self._wheel(nv, nh)

    def key(self, mods, usage, action):
        name = HID_NAMES.get(usage)
        mod_names = [n for bit, n in MOD_NAMES if mods & bit]
        if action == KEY_TAP:
            for m in mod_names:
                self._key(m, True)
            if name:
                self._key(name, True)
                self._key(name, False)
            for m in reversed(mod_names):
                self._key(m, False)
        elif action == KEY_DOWN:
            for m in mod_names:
                if m not in self._held_mods:
                    self._key(m, True)
                    self._held_mods.add(m)
            if name and name not in self._held_keys:
                self._key(name, True)
                self._held_keys.add(name)
        elif action == KEY_UP:
            if name and name in self._held_keys:
                self._key(name, False)
                self._held_keys.discard(name)
            for m in mod_names:
                if m in self._held_mods:
                    self._key(m, False)
                    self._held_mods.discard(m)

    def release_all(self):
        """Never leave a button or key stuck when a phone disappears."""
        try:
            self.buttons(0)
            for k in list(self._held_keys):
                self._key(k, False)
            for m in list(self._held_mods):
                self._key(m, False)
        finally:
            self._held_keys.clear()
            self._held_mods.clear()
            self._acc_v = self._acc_h = 0

    def close(self):
        self.release_all()


# =============================================================================
# Windows — SendInput
# =============================================================================
class WindowsInjector(Injector):
    name = "Windows SendInput"
    hires_scroll = True

    VK = {
        "enter": 0x0D, "esc": 0x1B, "backspace": 0x08, "tab": 0x09, "space": 0x20,
        "minus": 0xBD, "equal": 0xBB, "lbracket": 0xDB, "rbracket": 0xDD,
        "backslash": 0xDC, "semicolon": 0xBA, "quote": 0xDE, "grave": 0xC0,
        "comma": 0xBC, "period": 0xBE, "slash": 0xBF,
        "insert": 0x2D, "home": 0x24, "pageup": 0x21, "delete": 0x2E, "end": 0x23,
        "pagedown": 0x22, "right": 0x27, "left": 0x25, "down": 0x28, "up": 0x26,
        "ctrl": 0xA2, "shift": 0xA0, "alt": 0xA4, "meta": 0x5B,
    }
    EXTENDED = {"insert", "home", "pageup", "delete", "end", "pagedown",
                "right", "left", "down", "up", "meta"}

    def __init__(self):
        super().__init__()
        import ctypes
        from ctypes import wintypes

        ulong_ptr = ctypes.c_size_t

        class MOUSEINPUT(ctypes.Structure):
            _fields_ = [("dx", wintypes.LONG), ("dy", wintypes.LONG),
                        ("mouseData", wintypes.DWORD), ("dwFlags", wintypes.DWORD),
                        ("time", wintypes.DWORD), ("dwExtraInfo", ulong_ptr)]

        class KEYBDINPUT(ctypes.Structure):
            _fields_ = [("wVk", wintypes.WORD), ("wScan", wintypes.WORD),
                        ("dwFlags", wintypes.DWORD), ("time", wintypes.DWORD),
                        ("dwExtraInfo", ulong_ptr)]

        class HARDWAREINPUT(ctypes.Structure):
            _fields_ = [("uMsg", wintypes.DWORD), ("wParamL", wintypes.WORD),
                        ("wParamH", wintypes.WORD)]

        class _U(ctypes.Union):
            _fields_ = [("mi", MOUSEINPUT), ("ki", KEYBDINPUT), ("hi", HARDWAREINPUT)]

        class INPUT(ctypes.Structure):
            _fields_ = [("type", wintypes.DWORD), ("u", _U)]

        self._ct = ctypes
        self._INPUT = INPUT
        self._user32 = ctypes.WinDLL("user32", use_last_error=True)
        self._user32.SendInput.argtypes = (wintypes.UINT, ctypes.POINTER(INPUT), ctypes.c_int)
        self._user32.SendInput.restype = wintypes.UINT
        self._map_vk = self._user32.MapVirtualKeyW
        # Pre-allocated structures: no allocation in the hot path.
        self._mouse = INPUT(type=0)
        self._kbd = INPUT(type=1)
        self._size = ctypes.sizeof(INPUT)
        self._ptr_mouse = ctypes.byref(self._mouse)
        self._ptr_kbd = ctypes.byref(self._kbd)

    def _send_mouse(self, dx, dy, data, flags):
        mi = self._mouse.u.mi
        mi.dx, mi.dy, mi.mouseData, mi.dwFlags = dx, dy, data & 0xFFFFFFFF, flags
        self._user32.SendInput(1, self._ptr_mouse, self._size)

    def _move(self, dx, dy):
        self._send_mouse(dx, dy, 0, 0x0001)  # MOUSEEVENTF_MOVE

    _BTN_FLAGS = {"left": (0x0002, 0x0004), "right": (0x0008, 0x0010), "middle": (0x0020, 0x0040)}

    def _button(self, name, down):
        dn, up = self._BTN_FLAGS[name]
        self._send_mouse(0, 0, 0, dn if down else up)

    def _wheel(self, v, h):
        if v:
            self._send_mouse(0, 0, v, 0x0800)   # MOUSEEVENTF_WHEEL
        if h:
            self._send_mouse(0, 0, h, 0x01000)  # MOUSEEVENTF_HWHEEL

    def _key(self, name, down):
        if len(name) == 1:
            vk = ord(name.upper())
        elif name.startswith("f") and name[1:].isdigit():
            vk = 0x6F + int(name[1:])
        else:
            vk = self.VK.get(name)
        if vk is None:
            return
        ki = self._kbd.u.ki
        ki.wVk = vk
        ki.wScan = self._map_vk(vk, 0) & 0xFFFF
        flags = 0x0001 if name in self.EXTENDED else 0
        if not down:
            flags |= 0x0002
        ki.dwFlags = flags
        self._user32.SendInput(1, self._ptr_kbd, self._size)


# =============================================================================
# Linux — uinput (python-evdev)
# =============================================================================
class UinputInjector(Injector):
    name = "Linux uinput"
    hires_scroll = True

    def __init__(self):
        super().__init__()
        from evdev import UInput, ecodes as e
        self._e = e
        self._wheel_hi = getattr(e, "REL_WHEEL_HI_RES", None)
        self._hwheel_hi = getattr(e, "REL_HWHEEL_HI_RES", None)
        names = {
            "enter": "KEY_ENTER", "esc": "KEY_ESC", "backspace": "KEY_BACKSPACE",
            "tab": "KEY_TAB", "space": "KEY_SPACE", "minus": "KEY_MINUS",
            "equal": "KEY_EQUAL", "lbracket": "KEY_LEFTBRACE", "rbracket": "KEY_RIGHTBRACE",
            "backslash": "KEY_BACKSLASH", "semicolon": "KEY_SEMICOLON",
            "quote": "KEY_APOSTROPHE", "grave": "KEY_GRAVE", "comma": "KEY_COMMA",
            "period": "KEY_DOT", "slash": "KEY_SLASH", "insert": "KEY_INSERT",
            "home": "KEY_HOME", "pageup": "KEY_PAGEUP", "delete": "KEY_DELETE",
            "end": "KEY_END", "pagedown": "KEY_PAGEDOWN", "right": "KEY_RIGHT",
            "left": "KEY_LEFT", "down": "KEY_DOWN", "up": "KEY_UP",
            "ctrl": "KEY_LEFTCTRL", "shift": "KEY_LEFTSHIFT", "alt": "KEY_LEFTALT",
            "meta": "KEY_LEFTMETA",
        }
        for c in "abcdefghijklmnopqrstuvwxyz0123456789":
            names[c] = "KEY_" + c.upper()
        for i in range(1, 13):
            names["f%d" % i] = "KEY_F%d" % i
        self._keys = {k: getattr(e, v) for k, v in names.items()}
        self._btn = {"left": e.BTN_LEFT, "right": e.BTN_RIGHT, "middle": e.BTN_MIDDLE}
        rel = [e.REL_X, e.REL_Y, e.REL_WHEEL, e.REL_HWHEEL]
        rel += [c for c in (self._wheel_hi, self._hwheel_hi) if c is not None]
        caps = {e.EV_REL: rel, e.EV_KEY: list(self._btn.values()) + list(self._keys.values())}
        self._ui = UInput(caps, name="Volavo Virtual Mouse")
        self._acc_lo_v = 0
        self._acc_lo_h = 0

    def _move(self, dx, dy):
        e = self._e
        if dx:
            self._ui.write(e.EV_REL, e.REL_X, dx)
        if dy:
            self._ui.write(e.EV_REL, e.REL_Y, dy)
        self._ui.syn()

    def _button(self, name, down):
        self._ui.write(self._e.EV_KEY, self._btn[name], 1 if down else 0)
        self._ui.syn()

    def _wheel(self, v, h):
        e = self._e
        # Hi-res events for smooth scrolling + classic notches for legacy apps.
        self._acc_lo_v += v
        self._acc_lo_h += h
        nv, nh = int(self._acc_lo_v / NOTCH), int(self._acc_lo_h / NOTCH)
        self._acc_lo_v -= nv * NOTCH
        self._acc_lo_h -= nh * NOTCH
        if v and self._wheel_hi is not None:
            self._ui.write(e.EV_REL, self._wheel_hi, v)
        if h and self._hwheel_hi is not None:
            self._ui.write(e.EV_REL, self._hwheel_hi, h)
        if nv:
            self._ui.write(e.EV_REL, e.REL_WHEEL, nv)
        if nh:
            self._ui.write(e.EV_REL, e.REL_HWHEEL, nh)
        self._ui.syn()

    def _key(self, name, down):
        code = self._keys.get(name)
        if code is not None:
            self._ui.write(self._e.EV_KEY, code, 1 if down else 0)
            self._ui.syn()

    def close(self):
        super().close()
        self._ui.close()


# =============================================================================
# Generic — pynput (macOS / X11)
# =============================================================================
class PynputInjector(Injector):
    name = "pynput"

    def __init__(self):
        super().__init__()
        from pynput import keyboard, mouse
        self._mouse = mouse.Controller()
        self._kb = keyboard.Controller()
        K = keyboard.Key
        self._btn = {"left": mouse.Button.left, "right": mouse.Button.right,
                     "middle": mouse.Button.middle}
        chars = {"minus": "-", "equal": "=", "lbracket": "[", "rbracket": "]",
                 "backslash": "\\", "semicolon": ";", "quote": "'", "grave": "`",
                 "comma": ",", "period": ".", "slash": "/"}
        special = {"enter": K.enter, "esc": K.esc, "backspace": K.backspace,
                   "tab": K.tab, "space": K.space, "home": K.home, "pageup": K.page_up,
                   "delete": K.delete, "end": K.end, "pagedown": K.page_down,
                   "right": K.right, "left": K.left, "down": K.down, "up": K.up,
                   "ctrl": K.ctrl, "shift": K.shift, "alt": K.alt, "meta": K.cmd}
        if hasattr(K, "insert"):
            special["insert"] = K.insert
        for i in range(1, 13):
            special["f%d" % i] = getattr(K, "f%d" % i)
        self._keys = dict(special)
        self._keys.update(chars)
        for c in "abcdefghijklmnopqrstuvwxyz0123456789":
            self._keys[c] = c

    def _move(self, dx, dy):
        self._mouse.move(dx, dy)

    def _button(self, name, down):
        (self._mouse.press if down else self._mouse.release)(self._btn[name])

    def _wheel(self, v, h):
        self._mouse.scroll(h, v)

    def _key(self, name, down):
        k = self._keys.get(name)
        if k is not None:
            (self._kb.press if down else self._kb.release)(k)


# =============================================================================
# Dry run
# =============================================================================
class DryRunInjector(Injector):
    name = "dry-run"
    hires_scroll = True

    def __init__(self, log=print):
        super().__init__()
        self.events = []
        self._log = log

    def _emit(self, *ev):
        self.events.append(ev)
        if self._log:
            self._log("[dry-run] %s" % (ev,))

    def _move(self, dx, dy):
        self._emit("move", dx, dy)

    def _button(self, name, down):
        self._emit("button", name, down)

    def _wheel(self, v, h):
        self._emit("wheel", v, h)

    def _key(self, name, down):
        self._emit("key", name, down)


def create_injector(dry_run=False, log=print):
    if dry_run:
        return DryRunInjector(log=log)
    errors = []
    candidates = []
    if sys.platform.startswith("win"):
        candidates.append(WindowsInjector)
    elif sys.platform.startswith("linux"):
        candidates.append(UinputInjector)
    candidates.append(PynputInjector)
    for cls in candidates:
        try:
            return cls()
        except Exception as exc:  # noqa: BLE001 — try the next back-end
            errors.append("%s: %s" % (cls.name, exc))
    raise RuntimeError(
        "Aucun moteur d'injection disponible.\n  " + "\n  ".join(errors) +
        "\nInstallez les dépendances : pip install -r requirements.txt"
    )
