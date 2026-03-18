import io
class BtWrapper(io.RawIOBase):
    def __init__(self, kt): self.kt = kt
    def read(self, n=-1): return bytes(self.kt.readBytes(n))
    def write(self, b): 
        self.kt.writeBytes(bytes(b))
        return len(b)
    def readable(self): return True
    def writable(self): return True