#!/usr/bin/env python3
"""Vôlavo Hôte — companion for the Vôlavo phone mouse.

Run it on the computer, scan the QR code with the Vôlavo app, done.

    python volavo_host.py              # window with the QR code
    python volavo_host.py --headless   # QR code printed in the terminal
"""

import argparse
import hmac
import json
import os
import platform
import re
import socket
import subprocess
import sys
import threading
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import volavo_protocol as P  # noqa: E402
from injectors import create_injector  # noqa: E402

APP_NAME = "Vôlavo Hôte"
CONFIG_PATH = os.path.join(os.path.expanduser("~"), ".volavo_host.json")
SESSION_TIMEOUT_S = 4.0


# =============================================================================
# Configuration & environment discovery
# =============================================================================
def load_config():
    try:
        with open(CONFIG_PATH, "r", encoding="utf-8") as fh:
            return json.load(fh)
    except (OSError, ValueError):
        return {}


def save_config(cfg):
    try:
        with open(CONFIG_PATH, "w", encoding="utf-8") as fh:
            json.dump(cfg, fh, indent=2)
        if os.name != "nt":
            os.chmod(CONFIG_PATH, 0o600)
    except OSError as exc:
        print("Impossible d'enregistrer la configuration : %s" % exc)


def _is_usable_ipv4(ip):
    return bool(ip) and not ip.startswith("127.") and not ip.startswith("169.254.")


def get_lan_ips():
    """Primary LAN address first, then the other IPv4 addresses (max 4)."""
    ips = []
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect(("10.255.255.255", 1))  # no packet is actually sent
            ips.append(s.getsockname()[0])
        finally:
            s.close()
    except OSError:
        pass
    try:
        for info in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
            ips.append(info[4][0])
    except OSError:
        pass
    seen, result = set(), []
    for ip in ips:
        if _is_usable_ipv4(ip) and ip not in seen:
            seen.add(ip)
            result.append(ip)
    return result[:4] or ["127.0.0.1"]


_MAC_RE = re.compile(r"([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}")


def get_bluetooth_mac():
    """Best effort: address of the local Bluetooth adapter, or None."""
    try:
        system = platform.system()
        if system == "Linux":
            base = "/sys/class/bluetooth"
            for name in sorted(os.listdir(base)):
                path = os.path.join(base, name, "address")
                if os.path.exists(path):
                    with open(path) as fh:
                        mac = fh.read().strip()
                    if _MAC_RE.fullmatch(mac) and mac != "00:00:00:00:00:00":
                        return mac.upper()
        elif system == "Darwin":
            out = subprocess.run(["system_profiler", "SPBluetoothDataType"],
                                 capture_output=True, text=True, timeout=8).stdout
            for line in out.splitlines():
                if "Address" in line:
                    m = _MAC_RE.search(line)
                    if m:
                        return m.group(0).replace("-", ":").upper()
        elif system == "Windows":
            import winreg
            key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE,
                                 r"SYSTEM\CurrentControlSet\Services\BTHPORT\Parameters\Keys")
            name = winreg.EnumKey(key, 0)
            if re.fullmatch(r"[0-9a-fA-F]{12}", name):
                return ":".join(name[i:i + 2] for i in range(0, 12, 2)).upper()
    except Exception:  # noqa: BLE001 — Bluetooth is optional
        pass
    return None


# =============================================================================
# UDP server
# =============================================================================
class VolavoServer:
    def __init__(self, port, key, injector, log=print):
        self.port = port
        self.key = key
        self.injector = injector
        self.log = log
        self.client = None
        self.client_name = None
        self.last_seen = 0.0
        self.packets = 0
        self.rate_hz = 0
        self._rate_count = 0
        self._rate_t0 = time.monotonic()
        self._nack_times = {}
        self._running = False
        self._lock = threading.Lock()
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        if os.name == "nt":
            # Ignore ICMP "port unreachable" resets (WSAECONNRESET) on Windows.
            try:
                self.sock.ioctl(socket.SIO_UDP_CONNRESET, False)
            except (AttributeError, OSError):
                pass
        self.sock.bind(("0.0.0.0", port))
        self.sock.settimeout(0.5)
        self.hostname = socket.gethostname()

    # --- lifecycle ---------------------------------------------------------
    def start(self):
        self._running = True
        t = threading.Thread(target=self._loop, name="volavo-udp", daemon=True)
        t.start()
        return t

    def stop(self):
        self._running = False
        self._drop_client("arrêt")
        try:
            self.sock.close()
        except OSError:
            pass

    def set_key(self, key):
        with self._lock:
            self.key = key
        self._drop_client("nouvelle clé")

    def status(self):
        return {
            "client": self.client,
            "name": self.client_name,
            "rate": self.rate_hz,
            "packets": self.packets,
        }

    # --- internals ---------------------------------------------------------
    def _drop_client(self, reason):
        if self.client is not None:
            self.log("Téléphone déconnecté (%s) : %s" % (reason, self.client_name))
        self.client = None
        self.client_name = None
        try:
            self.injector.release_all()
        except Exception as exc:  # noqa: BLE001
            self.log("release_all: %s" % exc)

    def _send(self, data, addr):
        try:
            self.sock.sendto(data, addr)
        except OSError:
            pass

    def _handle_hello(self, data, addr):
        if len(data) < 2 + P.KEY_LEN:
            return
        version = data[1]
        if version != P.VERSION:
            self._send(bytes([P.T_HELLO_ACK, P.ACK_BAD_VERSION]), addr)
            return
        key = data[2:2 + P.KEY_LEN]
        with self._lock:
            expected = self.key.encode("ascii")
        if not hmac.compare_digest(key, expected):
            self.log("Clé refusée depuis %s:%d" % addr)
            self._send(bytes([P.T_HELLO_ACK, P.ACK_BAD_KEY]), addr)
            return
        name = data[2 + P.KEY_LEN:2 + P.KEY_LEN + 64].decode("utf-8", "replace") or "Téléphone"
        if self.client != addr:
            if self.client is not None:
                self._drop_client("remplacé")
            self.client = addr
            self.client_name = name
            self.log("Téléphone connecté : %s (%s:%d)" % (name, addr[0], addr[1]))
        self.last_seen = time.monotonic()
        self._send(bytes([P.T_HELLO_ACK, P.ACK_OK]) + self.hostname.encode("utf-8")[:60], addr)

    def _loop(self):
        inj = self.injector
        move = P.S_MOVE.unpack_from
        scroll = P.S_SCROLL.unpack_from
        key = P.S_KEY.unpack_from
        while self._running:
            try:
                data, addr = self.sock.recvfrom(512)
            except socket.timeout:
                if self.client and time.monotonic() - self.last_seen > SESSION_TIMEOUT_S:
                    self._drop_client("délai dépassé")
                self._update_rate()
                continue
            except (ConnectionResetError, OSError):
                if not self._running:
                    break
                continue
            if not data:
                continue
            t = data[0]
            if t == P.T_HELLO:
                self._handle_hello(data, addr)
                continue
            if addr != self.client:
                now = time.monotonic()
                if now - self._nack_times.get(addr, 0) > 0.5:
                    self._nack_times[addr] = now
                    self._send(bytes([P.T_NEED_AUTH]), addr)
                continue
            self.last_seen = time.monotonic()
            self.packets += 1
            self._rate_count += 1
            try:
                if t == P.T_MOVE and len(data) >= 5:
                    dx, dy = move(data, 1)
                    inj.move(dx, dy)
                elif t == P.T_BUTTONS and len(data) >= 2:
                    inj.buttons(data[1] & 0x07)
                elif t == P.T_SCROLL and len(data) >= 5:
                    v, h = scroll(data, 1)
                    inj.scroll(v, h)
                elif t == P.T_KEY and len(data) >= 4:
                    mods, usage, action = key(data, 1)
                    inj.key(mods, usage, action)
                elif t == P.T_PING:
                    self._send(bytes([P.T_PONG]) + data[1:64], addr)
                elif t == P.T_BYE:
                    self._drop_client("fermeture")
            except Exception as exc:  # noqa: BLE001 — never kill the loop
                self.log("Erreur d'injection : %s" % exc)
            self._update_rate()

    def _update_rate(self):
        now = time.monotonic()
        dt = now - self._rate_t0
        if dt >= 1.0:
            self.rate_hz = int(self._rate_count / dt)
            self._rate_count = 0
            self._rate_t0 = now


# =============================================================================
# QR code helpers
# =============================================================================
def qr_matrix(text):
    try:
        import qrcode
    except ImportError:
        return None
    qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, border=2)
    qr.add_data(text)
    qr.make(fit=True)
    return qr.get_matrix()


def print_qr(text):
    matrix = qr_matrix(text)
    if matrix is None:
        print("(Installez 'qrcode' pour afficher le QR : pip install qrcode)")
        return
    # Two matrix rows per text line using half blocks; dark modules = black.
    lines = []
    for y in range(0, len(matrix), 2):
        top = matrix[y]
        bottom = matrix[y + 1] if y + 1 < len(matrix) else [False] * len(top)
        row = []
        for a, b in zip(top, bottom):
            row.append({(False, False): "█", (True, False): "▄",
                        (False, True): "▀", (True, True): " "}[(a, b)])
        lines.append("".join(row))
    try:
        print("\n".join(lines))
    except UnicodeEncodeError:
        for y in range(len(matrix)):
            print("".join("  " if c else "##" for c in matrix[y]))


# =============================================================================
# GUI
# =============================================================================
class HostWindow:
    BG = "#0F1015"
    SURFACE = "#161822"
    CYAN = "#00E5FF"
    GREEN = "#00E676"
    TEXT = "#F1F5F9"
    MUTED = "#94A3B8"

    def __init__(self, app):
        import tkinter as tk
        self.tk = tk
        self.app = app
        self.root = tk.Tk()
        self.root.title(APP_NAME)
        self.root.configure(bg=self.BG)
        self.root.resizable(False, False)
        self.root.protocol("WM_DELETE_WINDOW", self.quit)

        tk.Label(self.root, text="Vôlavo", fg=self.CYAN, bg=self.BG,
                 font=("Helvetica", 22, "bold")).pack(pady=(16, 0))
        tk.Label(self.root, text="Scannez ce code avec l'application Vôlavo",
                 fg=self.MUTED, bg=self.BG, font=("Helvetica", 11)).pack(pady=(2, 10))

        self.canvas = tk.Canvas(self.root, width=300, height=300, bg="white",
                                highlightthickness=0)
        self.canvas.pack(padx=24)

        self.status = tk.Label(self.root, text="", fg=self.MUTED, bg=self.BG,
                               font=("Helvetica", 12, "bold"))
        self.status.pack(pady=(12, 0))
        self.details = tk.Label(self.root, text="", fg=self.MUTED, bg=self.BG,
                                font=("Courier", 10), justify="center")
        self.details.pack(pady=(4, 8))

        bar = tk.Frame(self.root, bg=self.BG)
        bar.pack(pady=(0, 16))
        for label, cmd in (("Copier le lien", self.copy_link),
                           ("Nouvelle clé", self.app.regenerate_key),
                           ("Quitter", self.quit)):
            tk.Button(bar, text=label, command=cmd, relief="flat", bg=self.SURFACE,
                      fg=self.TEXT, activebackground="#2A2D3F", activeforeground=self.CYAN,
                      padx=10, pady=4).pack(side="left", padx=4)
        self.refresh_qr()
        self._tick()

    def refresh_qr(self):
        self.canvas.delete("all")
        matrix = qr_matrix(self.app.uri)
        if matrix is None:
            self.canvas.create_text(150, 150, width=260, justify="center",
                                    text="Module 'qrcode' manquant.\npip install qrcode")
            return
        n = len(matrix)
        cell = 300 / n
        for y, row in enumerate(matrix):
            x0 = None
            for x, dark in enumerate(row + [False]):
                if dark and x0 is None:
                    x0 = x
                elif not dark and x0 is not None:
                    self.canvas.create_rectangle(x0 * cell, y * cell, x * cell, (y + 1) * cell,
                                                 fill="black", outline="black")
                    x0 = None

    def copy_link(self):
        self.root.clipboard_clear()
        self.root.clipboard_append(self.app.uri)

    def _tick(self):
        st = self.app.server.status()
        if st["client"]:
            self.status.config(text="● Connecté : %s" % st["name"], fg=self.GREEN)
        else:
            self.status.config(text="En attente du téléphone…", fg=self.MUTED)
        lines = ["Adresse : %s  port %d" % (", ".join(self.app.ips), self.app.port),
                 "Code manuel : %s" % P.format_key(self.app.key)]
        if self.app.bt_mac:
            lines.append("Bluetooth : %s" % self.app.bt_mac)
        if st["client"]:
            lines.append("%d paquets/s • moteur %s" % (st["rate"], self.app.injector.name))
        self.details.config(text="\n".join(lines))
        self.root.after(300, self._tick)

    def quit(self):
        self.app.shutdown()
        self.root.destroy()

    def run(self):
        self.root.mainloop()


# =============================================================================
# Application
# =============================================================================
class HostApp:
    def __init__(self, args):
        self.args = args
        self.cfg = load_config()
        if args.new_key or not self.cfg.get("key"):
            self.cfg["key"] = P.new_key()
        self.key = P.normalize_key(self.cfg["key"])
        self.port = args.port or int(self.cfg.get("port", P.DEFAULT_PORT))
        self.cfg["port"] = self.port
        save_config(self.cfg)

        self.ips = get_lan_ips()
        self.bt_mac = None if args.no_bt else (args.bt_mac or get_bluetooth_mac())
        self.name = args.name or socket.gethostname()
        self.injector = create_injector(dry_run=args.dry_run)
        try:
            self.server = VolavoServer(self.port, self.key, self.injector)
        except OSError as exc:
            sys.exit("Impossible d'ouvrir le port UDP %d (%s). "
                     "Vôlavo Hôte est peut-être déjà lancé." % (self.port, exc))
        self.uri = self._build_uri()

    def _build_uri(self):
        return P.pairing_uri(self.ips, self.port, self.key, self.name, self.bt_mac)

    def regenerate_key(self):
        self.key = P.new_key()
        self.cfg["key"] = self.key
        save_config(self.cfg)
        self.server.set_key(self.key)
        self.uri = self._build_uri()
        print("Nouvelle clé générée — rescannez le QR code.")
        if getattr(self, "window", None):
            self.window.refresh_qr()

    def shutdown(self):
        self.server.stop()
        self.injector.close()

    def run(self):
        print("%s — moteur d'injection : %s" % (APP_NAME, self.injector.name))
        print("Écoute UDP sur %s port %d" % (", ".join(self.ips), self.port))
        print("Code manuel : %s" % P.format_key(self.key))
        if self.bt_mac:
            print("Bluetooth : %s" % self.bt_mac)
        print("Lien d'appairage : %s" % self.uri)
        self.server.start()

        self.window = None
        if not self.args.headless:
            try:
                self.window = HostWindow(self)
            except Exception as exc:  # noqa: BLE001 — no display / no tkinter
                print("Interface graphique indisponible (%s), mode terminal." % exc)
        if self.window:
            self.window.run()
            return
        print()
        print_qr(self.uri)
        print("\nScannez ce code avec Vôlavo. Ctrl+C pour quitter.")
        try:
            while True:
                time.sleep(1)
        except KeyboardInterrupt:
            pass
        finally:
            self.shutdown()


def parse_args(argv=None):
    ap = argparse.ArgumentParser(description="Vôlavo Hôte — souris téléphone ultra-rapide")
    ap.add_argument("--port", type=int, default=0, help="port UDP (défaut %d)" % P.DEFAULT_PORT)
    ap.add_argument("--headless", action="store_true", help="pas de fenêtre, QR dans le terminal")
    ap.add_argument("--new-key", action="store_true", help="génère une nouvelle clé d'appairage")
    ap.add_argument("--name", help="nom affiché sur le téléphone")
    ap.add_argument("--bt-mac", help="adresse Bluetooth du PC à inclure dans le QR")
    ap.add_argument("--no-bt", action="store_true", help="ne pas inclure l'adresse Bluetooth")
    ap.add_argument("--dry-run", action="store_true", help="affiche les événements sans les injecter")
    return ap.parse_args(argv)


def _ensure_std_streams():
    """Windowed executables (PyInstaller --windowed) have no console: sys.stdout is None."""
    for name in ("stdout", "stderr"):
        if getattr(sys, name) is None:
            setattr(sys, name, open(os.devnull, "w", encoding="utf-8"))


def main(argv=None):
    _ensure_std_streams()
    HostApp(parse_args(argv)).run()


if __name__ == "__main__":
    main()
