import RNS
from utils.image_handler import compress_image

class ChatManager:
    def __init__(self, callback_obj):
        self.cb = callback_obj
        RNS.Reticulum()
        self.identity = RNS.Identity()
        self.destination = RNS.Destination(self.identity, RNS.Destination.IN, RNS.Destination.SINGLE, "chat")
        self.destination.set_packet_callback(self.on_packet)

    def on_packet(self, data, packet):
        self.cb.onMessage(data.decode("utf-8"))

    def send_text(self, target_hex, text):
        target_hash = bytes.fromhex(target_hex)
        out_dest = RNS.Destination(None, RNS.Destination.OUT, RNS.Destination.SINGLE, "chat")
        out_dest.hash = target_hash
        RNS.Packet(out_dest, text.encode("utf-8")).send()