import RNS
import os

class ChatManager:
    def __init__(self, callback_obj, mac_address):
        self.cb = callback_obj
        
        # Setup RNS Config Directory
        config_dir = os.path.expanduser("~/.reticulum")
        if not os.path.exists(config_dir):
            os.makedirs(config_dir)

        # Create the Bluetooth Config
        config_content = f"""
[reticulum]
[logging]
loglevel = 4

[interfaces]
  [[RNode BT Interface]]
    type = BluetoothInterface
    enabled = yes
    outgoing = yes
    mac = {mac_address}
"""
        with open(os.path.join(config_dir, "config"), "w") as f:
            f.write(config_content)

        # Initialize Reticulum
        RNS.Reticulum()
        self.identity = RNS.Identity()
        self.destination = RNS.Destination(self.identity, RNS.Destination.IN, RNS.Destination.SINGLE, "chat")
        self.destination.set_packet_callback(self.on_packet)
        
        self.cb.onLog(f"RNS Started on {mac_address}")
        self.cb.onLog(f"My Hash: {RNS.hexrep(self.destination.hash)}")

    def on_packet(self, data, packet):
        self.cb.onMessage(data.decode("utf-8"))

    def send_text(self, target_hex, text):
        try:
            target_hash = bytes.fromhex(target_hex)
            out_dest = RNS.Destination(None, RNS.Destination.OUT, RNS.Destination.SINGLE, "chat")
            out_dest.hash = target_hash
            RNS.Packet(out_dest, text.encode("utf-8")).send()
        except Exception as e:
            self.cb.onLog(f"Error: {str(e)}")