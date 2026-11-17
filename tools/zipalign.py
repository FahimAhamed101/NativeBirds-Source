"""4-byte zip alignment for an APK, without needing zipalign.exe.

zipalign.exe cannot be executed in this environment (process creation is
blocked), so this rewrites the archive itself: every entry's data offset is
padded to a 4-byte boundary using Android's own padding extra field (0xD935),
and the compressed bytes are copied through untouched so the payload is
bit-identical to what was packaged.

usage: python tools/zipalign.py in.apk out.apk
"""
import struct
import sys
import zipfile

PAD_ID = 0xD935


def dos_time(date_time):
    """(year, month, day, hour, min, sec) -> (dos time, dos date)."""
    year, month, day, hour, minute, second = date_time
    if year < 1980:
        year, month, day = 1980, 1, 1
    return ((hour << 11) | (minute << 5) | (second // 2),
            ((year - 1980) << 9) | (month << 5) | day)


def align(src, dst):
    raw = open(src, "rb").read()
    zin = zipfile.ZipFile(src, "r")

    out = bytearray()
    central = []

    for info in zin.infolist():
        name = info.filename.encode("utf-8")

        # Locate this entry's compressed payload in the source archive.
        off = info.header_offset + 30 + len(name) + len(info.extra)
        data = raw[off:off + info.compress_size]

        base = len(out) + 30 + len(name)
        pad = (4 - (base % 4)) % 4
        if pad == 0:
            extra = b""
        else:
            extra_len = pad if pad >= 4 else pad + 4
            extra = struct.pack("<HH", PAD_ID, extra_len - 4) + b"\x00" * (extra_len - 4)

        offset = len(out)
        modtime, moddate = dos_time(info.date_time)
        out += struct.pack("<IHHHHHIIIHH", 0x04034B50, info.extract_version,
                           info.flag_bits, info.compress_type,
                           modtime, moddate,
                           info.CRC, info.compress_size, info.file_size,
                           len(name), len(extra))
        out += name + extra + data

        central.append((name, extra, offset, info, modtime, moddate))

    cd_start = len(out)
    for name, extra, offset, info, modtime, moddate in central:
        out += struct.pack("<IHHHHHHIIIHHHHHII", 0x02014B50, info.create_version,
                           info.extract_version, info.flag_bits, info.compress_type,
                           modtime, moddate,
                           info.CRC, info.compress_size, info.file_size,
                           len(name), len(extra), 0, 0, 0, 0, offset)
        out += name + extra

    cd_size = len(out) - cd_start
    out += struct.pack("<IHHHHIIH", 0x06054B50, 0, 0, len(central), len(central),
                       cd_size, cd_start, 0)

    open(dst, "wb").write(bytes(out))

    zin.close()
    return len(central)


if __name__ == "__main__":
    n = align(sys.argv[1], sys.argv[2])
    print("aligned %d entries -> %s" % (n, sys.argv[2]))
