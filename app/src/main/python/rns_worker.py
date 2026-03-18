import RNS, os, base64

reticulum, my_dest = None, None

def start(bt_wrap):
    global reticulum, my_dest
    config_dir = os.path.expanduser("~/.reticulum")
    os.makedirs(config_dir, exist_ok=True)
    reticulum = RNS.Reticulum(configdir=config_dir)
    # Applied your parameters: 433.025MHz, BW 125k, SF 8, CR 6, TX 17
    inter = RNS.Interfaces.RNodeInterface.RNodeInterface(
        None, name="RNodeBT", port=bt_wrap, 
        frequency=433025000, bandwidth=125000, txpower=17, sf=8, cr=6
    )
    reticulum.interfaces.append(inter)
    my_dest = RNS.Destination(RNS.Identity(), RNS.Destination.IN, RNS.Destination.SINGLE, "chat")
    my_dest.announce()
    return RNS.hexrep(my_dest.hash)

def send_message(dest, txt):
    d = RNS.Destination(None, RNS.Destination.OUT, RNS.Destination.SINGLE, "chat")
    d.hash = bytes.fromhex(dest)
    RNS.Packet(d, txt.encode()).send()
    return "Sent"

def send_image(dest, b64):
    d = RNS.Destination(None, RNS.Destination.OUT, RNS.Destination.SINGLE, "chat")
    d.hash = bytes.fromhex(dest)
    RNS.Resource(base64.b64decode(b64), d)
    return "Image Sending"