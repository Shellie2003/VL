"""Vôlavo wire protocol (v1) — shared constants and helpers.

Every datagram starts with a one-byte type. All integers are big-endian.

Phone -> host
  0x01 HELLO    u8 version, 16 bytes ASCII pairing key, UTF-8 device name
  0x02 MOVE     i16 dx, i16 dy                       (relative pixels)
  0x03 BUTTONS  u8 mask (bit0 left, bit1 right, bit2 middle) — full state
  0x04 SCROLL   i16 vertical, i16 horizontal         (1/120 of a notch)
  0x05 KEY      u8 modifiers (HID bits), u8 HID usage, u8 action (0 tap, 1 down, 2 up)
  0x06 PING     opaque payload (echoed back verbatim)
  0x07 BYE
  0x08 KEY_PHYS u8 modifiers, u8 HID usage, u8 action — *physical* key position
                (interpreted by the computer's own layout: AZERTY, QWERTY…)
  0x09 TEXT     UTF-8 text to type as-is (any character, accents, emoji)
  0x0A MEDIA    u16 HID consumer usage (volume, play/pause, next…)
  0x0B MIDI     raw MIDI channel messages (note on/off, pitch bend, CC…) for the DAW

Host -> phone
  0x81 HELLO_ACK  u8 status (0 ok, 1 bad key, 2 bad version), UTF-8 host name
  0x86 PONG       echo of the PING payload
  0x8F NEED_AUTH  sent when an unknown address talks without a HELLO
"""

import base64
import secrets
import struct
from urllib.parse import quote

VERSION = 1
DEFAULT_PORT = 50505
KEY_LEN = 16

T_HELLO = 0x01
T_MOVE = 0x02
T_BUTTONS = 0x03
T_SCROLL = 0x04
T_KEY = 0x05
T_PING = 0x06
T_BYE = 0x07
T_KEY_PHYS = 0x08
T_TEXT = 0x09
T_MEDIA = 0x0A
T_MIDI = 0x0B

T_HELLO_ACK = 0x81
T_PONG = 0x86
T_NEED_AUTH = 0x8F

ACK_OK = 0
ACK_BAD_KEY = 1
ACK_BAD_VERSION = 2

KEY_TAP = 0
KEY_DOWN = 1
KEY_UP = 2

BTN_LEFT = 0x01
BTN_RIGHT = 0x02
BTN_MIDDLE = 0x04

MOD_CTRL = 0x01
MOD_SHIFT = 0x02
MOD_ALT = 0x04
MOD_META = 0x08
MOD_RALT = 0x40  # AltGr

S_U16 = struct.Struct("!H")

S_MOVE = struct.Struct("!hh")
S_SCROLL = struct.Struct("!hh")
S_KEY = struct.Struct("!BBB")


def new_key() -> str:
    """16 characters of base32 (80 bits of entropy), easy to type if needed."""
    return base64.b32encode(secrets.token_bytes(10)).decode("ascii")


def normalize_key(key: str) -> str:
    return key.replace("-", "").replace(" ", "").upper()


def format_key(key: str) -> str:
    return "-".join(key[i:i + 4] for i in range(0, len(key), 4))


def pairing_uri(hosts, port, key, name, bt_mac=None) -> str:
    uri = "volavo://pair?v={}&h={}&p={}&k={}&n={}".format(
        VERSION, ",".join(hosts), port, key, quote(name, safe="")
    )
    if bt_mac:
        uri += "&b=" + bt_mac.upper()
    return uri
