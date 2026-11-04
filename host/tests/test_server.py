"""End-to-end tests: a fake phone talks to the real UDP server (dry-run injector)."""
import os
import socket
import struct
import sys
import time
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import volavo_protocol as P  # noqa: E402
from injectors import DryRunInjector, HID_NAMES  # noqa: E402
from volavo_host import VolavoServer  # noqa: E402


class FakePhone:
    def __init__(self, port):
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        self.sock.settimeout(1.0)
        self.sock.connect(("127.0.0.1", port))

    def send(self, data):
        self.sock.send(data)

    def recv(self):
        return self.sock.recv(512)

    def hello(self, key, version=P.VERSION):
        self.send(bytes([P.T_HELLO, version]) + key.encode() + "Pixel".encode())
        return self.recv()


def wait_for(pred, timeout=1.0):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        if pred():
            return True
        time.sleep(0.005)
    return False


class ServerTest(unittest.TestCase):
    def setUp(self):
        self.key = P.new_key()
        self.inj = DryRunInjector(log=None)
        self.server = VolavoServer(0, self.key, self.inj, log=lambda *_: None)
        self.port = self.server.sock.getsockname()[1]
        self.server.start()
        self.phone = FakePhone(self.port)

    def tearDown(self):
        self.server.stop()
        self.phone.sock.close()

    def test_bad_key_rejected(self):
        ack = self.phone.hello("A" * 16)
        self.assertEqual(ack[:2], bytes([P.T_HELLO_ACK, P.ACK_BAD_KEY]))
        self.phone.send(bytes([P.T_MOVE]) + P.S_MOVE.pack(10, 10))
        self.assertEqual(self.phone.recv(), bytes([P.T_NEED_AUTH]))
        self.assertEqual(self.inj.events, [])

    def test_bad_version(self):
        ack = self.phone.hello(self.key, version=99)
        self.assertEqual(ack[:2], bytes([P.T_HELLO_ACK, P.ACK_BAD_VERSION]))

    def test_full_session(self):
        ack = self.phone.hello(self.key)
        self.assertEqual(ack[:2], bytes([P.T_HELLO_ACK, P.ACK_OK]))
        self.assertEqual(self.server.client_name, "Pixel")

        self.phone.send(bytes([P.T_MOVE]) + P.S_MOVE.pack(-300, 42))
        self.phone.send(bytes([P.T_BUTTONS, P.BTN_LEFT]))
        self.phone.send(bytes([P.T_BUTTONS, P.BTN_LEFT]))  # duplicate is idempotent
        self.phone.send(bytes([P.T_BUTTONS, 0]))
        self.phone.send(bytes([P.T_SCROLL]) + P.S_SCROLL.pack(-120, 60))
        self.phone.send(bytes([P.T_KEY]) + P.S_KEY.pack(P.MOD_CTRL, 0x1D, P.KEY_TAP))  # Ctrl+Z
        ping = struct.pack("!IQ", 7, 123456789)
        self.phone.send(bytes([P.T_PING]) + ping)
        self.assertEqual(self.phone.recv(), bytes([P.T_PONG]) + ping)

        expected = [
            ("move", -300, 42),
            ("button", "left", True),
            ("button", "left", False),
            ("wheel", -120, 60),
            ("key", "ctrl", True), ("key", "z", True), ("key", "z", False), ("key", "ctrl", False),
        ]
        self.assertTrue(wait_for(lambda: len(self.inj.events) >= len(expected)))
        self.assertEqual(self.inj.events, expected)

    def test_keyboard_text_and_media(self):
        self.phone.hello(self.key)
        # AltGr + physical key 0x27 (AZERTY "@" position)
        self.phone.send(bytes([P.T_KEY_PHYS]) + P.S_KEY.pack(P.MOD_RALT, 0x27, P.KEY_TAP))
        self.phone.send(bytes([P.T_TEXT]) + "Été à Noël ✓\n".encode("utf-8"))
        self.phone.send(bytes([P.T_MEDIA]) + P.S_U16.pack(0xE9))
        # Win alone (modifier-only tap opens the Start menu)
        self.phone.send(bytes([P.T_KEY]) + P.S_KEY.pack(P.MOD_META, 0, P.KEY_TAP))
        expected = [
            ("key", "altgr", True), ("phys", 0x27, True), ("phys", 0x27, False), ("key", "altgr", False),
            ("text", "Été à Noël ✓\n"),
            ("media", "volup"),
            ("key", "meta", True), ("key", "meta", False),
        ]
        self.assertTrue(wait_for(lambda: len(self.inj.events) >= len(expected)))
        self.assertEqual(self.inj.events, expected)

    def test_held_state_released_on_bye(self):
        self.phone.hello(self.key)
        self.phone.send(bytes([P.T_BUTTONS, P.BTN_RIGHT]))
        self.phone.send(bytes([P.T_KEY]) + P.S_KEY.pack(P.MOD_SHIFT, 0x2C, P.KEY_DOWN))
        self.phone.send(bytes([P.T_BYE]))
        self.assertTrue(wait_for(lambda: ("button", "right", False) in self.inj.events))
        self.assertIn(("key", "space", False), self.inj.events)
        self.assertIn(("key", "shift", False), self.inj.events)
        self.assertIsNone(self.server.client)

    def test_key_regeneration_drops_client(self):
        self.phone.hello(self.key)
        self.server.set_key(P.new_key())
        self.assertIsNone(self.server.client)
        self.phone.send(bytes([P.T_MOVE]) + P.S_MOVE.pack(1, 1))
        self.assertEqual(self.phone.recv(), bytes([P.T_NEED_AUTH]))


class InjectorTest(unittest.TestCase):
    def test_lowres_scroll_accumulates(self):
        inj = DryRunInjector(log=None)
        inj.hires_scroll = False
        for _ in range(5):
            inj.scroll(30, 0)
        self.assertEqual(inj.events, [("wheel", 1, 0)])

    def test_hid_table(self):
        self.assertEqual(HID_NAMES[0x04], "a")
        self.assertEqual(HID_NAMES[0x27], "0")
        self.assertEqual(HID_NAMES[0x45], "f12")
        self.assertEqual(HID_NAMES[0x4F], "right")

    def test_pairing_uri(self):
        uri = P.pairing_uri(["192.168.1.2", "10.0.0.2"], 50505, "ABCD" * 4, "Mon PC", "aa:bb:cc:dd:ee:ff")
        self.assertEqual(
            uri,
            "volavo://pair?v=1&h=192.168.1.2,10.0.0.2&p=50505&k=ABCDABCDABCDABCD&n=Mon%20PC&b=AA:BB:CC:DD:EE:FF",
        )
        self.assertEqual(len(P.new_key()), 16)


if __name__ == "__main__":
    unittest.main()
