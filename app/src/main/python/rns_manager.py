import RNS
import os
import time

class ChatManager:
    def __init__(self, callback_obj):
        self.cb = callback_obj
        config_dir = os.path.expanduser("~/.reticulum")
        os.makedirs(config_dir, exist_ok=True)

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
        
        # Identity and Destination
        self.identity = RNS.Identity()
        # Sideband usually likes "official" app names, but "chat" is fine.
        self.destination = RNS.Destination(
            self.identity, 
            RNS.Destination.IN, 
            RNS.Destination.SINGLE, 
            "chat"
        )
        self.destination.set_packet_callback(self.on_packet)

        # --- THE FIX: ANNOUNCE ---
        # This tells the network (and Sideband) that we exist.
        self.destination.announce()
        
        self.cb.onLog(f"RNS Online.\nMy Hash: {RNS.hexrep(self.destination.hash)}")
        self.cb.onLog("Announce sent to network.")

    def announce_self(self):
        """Manually trigger an announce"""
        self.destination.announce()
        self.cb.onLog("Manual Announce sent.")

    def on_packet(self, data, packet):
        self.cb.onMessage(data.decode("utf-8"))

    def send_text(self, target_hex, text):
        try:
            # We must use RNS.hexrep to convert the string back to bytes
            dest_hash = bytes.fromhex(target_hex)
            # Create a generic outgoing destination
            out_dest = RNS.Destination(None, RNS.Destination.OUT, RNS.Destination.SINGLE, "chat")
            out_dest.hash = dest_hash
            
            RNS.Packet(out_dest, text.encode("utf-8")).send()
        except Exception as e:
            self.cb.onLog(f"Send Error: {str(e)}")