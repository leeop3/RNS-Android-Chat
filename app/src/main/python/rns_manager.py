import RNS
import os

class ChatManager:
    def __init__(self, callback_obj):
        self.cb = callback_obj
        config_dir = os.path.expanduser("~/.reticulum")
        os.makedirs(config_dir, exist_ok=True)

        # Connect to the local Kotlin Bridge on port 4242
        config_content = """
[reticulum]
[logging]
loglevel = 4
[interfaces]
  [[RNode Bridge]]
    type = TCPClientInterface
    enabled = yes
    target_host = 127.0.0.1
    target_port = 4242
"""
        with open(os.path.join(config_dir, "config"), "w") as f:
            f.write(config_content)

        RNS.Reticulum()
        self.identity = RNS.Identity()
        self.destination = RNS.Destination(self.identity, RNS.Destination.IN, RNS.Destination.SINGLE, "chat")
        self.destination.set_packet_callback(self.on_packet)
        
        self.cb.onLog(f"RNS Online.\nMy Hash: {RNS.hexrep(self.destination.hash)}")

    def on_packet(self, data, packet):
        self.cb.onMessage(data.decode("utf-8"))

    def send_text(self, target_hex, text):
        try:
            dest = RNS.Destination(None, RNS.Destination.OUT, RNS.Destination.SINGLE, "chat")
            dest.hash = bytes.fromhex(target_hex)
            RNS.Packet(dest, text.encode("utf-8")).send()
        except Exception as e:
            self.cb.onLog(f"Error: {str(e)}")