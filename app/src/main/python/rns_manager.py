import RNS
import os

class ChatManager:
    def __init__(self, callback_obj, mac_address):
        self.cb = callback_obj
        
        # 1. Define the RNS Config folder
        storage_path = os.path.expanduser("~/.reticulum")
        if not os.path.exists(storage_path):
            os.makedirs(storage_path)

        # 2. Create the config file with the Bluetooth Interface
        # We use the MAC address provided from the Kotlin UI
        config_content = f"""
[reticulum]
[logging]
loglevel = 4

[interfaces]
  [[RNode BT]]
    type = BluetoothInterface
    enabled = yes
    outgoing = yes
    mac = {mac_address}
"""
        with open(os.path.join(storage_path, "config"), "w") as f:
            f.write(config_content)

        # 3. Start Reticulum with the new config
        RNS.Reticulum()
        
        self.identity = RNS.Identity()
        self.destination = RNS.Destination(
            self.identity, 
            RNS.Destination.IN, 
            RNS.Destination.SINGLE, 
            "chat"
        )
        self.destination.set_packet_callback(self.on_packet)
        
        # Inform the UI that RNS is up
        self.cb.onLog(f"RNS started on {mac_address}")
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
            self.cb.onLog(f"Send Error: {str(e)}")