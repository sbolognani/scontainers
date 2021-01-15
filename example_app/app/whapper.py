import os
import sys
import time
import math
import random
from math import floor
from pprint import pprint
from threading import Thread

import redis
from flask_socketio import SocketIO

from webwhatsapi import WhatsAPIDriver, WhatsAPIDriverStatus, Contact
from webwhatsapi.objects.chat import Chat
from webwhatsapi.objects.message import Message

"Certains t'ont promis la terre"
"D'autres promettent le ciel"
"Il y en a qui t'ont promis la lune"
"Et moi je n'ai rien que ma pauvre guitare"




# ==============================================================================
def track(fn):
    def wrapper(*args, **kwargs):
        ts = time.perf_counter()
        result = fn(*args, **kwargs)
        te = time.perf_counter()
        print(f'{fn.__name__}: {te - ts:0.8f}')
        return result
    return wrapper


def threaded(fn):
    def wrapper(*args, **kwargs) -> Thread:
        thread = Thread(target=fn, args=args, kwargs=kwargs)
        thread.start()
        return thread
    return wrapper
# ==============================================================================

# ==============================================================================

# === What is the redis connection like?
# import redis
# a = redis.Redis(host='redis', port=6379)
# print(a.connection_pool.connection_kwargs)

# ==== Become a client?
import socketio
client_io = socketio.Client(ssl_verify=False, logger=False, engineio_logger=False)
client_io.connect('http://0.0.0.0:7777', namespaces=['/test'])
client_io.emit(event='driver_client', namespace='/test')

# sio.disconnect()

# ==============================================================================

PATH_TO_STATIC = 'app/static'
PATH_TO_IMAGES = PATH_TO_STATIC + '/images'
PROFILE_DIR = 'firefox_cache'
NAMESPACE = '/test'

io = SocketIO(message_queue='redis://redis:6379')
io.emit('marco', namespace=NAMESPACE, broadcast=True, include_self=True)
print('broadcasters')


from selenium.common.exceptions import TimeoutException

import logging

class NewMessageObserver:

    def on_message_received(self, new_messages):
        for message in new_messages:
            if message.type == "chat":
                print(
                    "New message '{}' received from number {}".format(
                        message.content, message.sender.id
                    )
                )
            else:
                print(
                    "New message of type '{}' received from number {}".format(
                        message.type, message.sender.id
                    )
                )


class Driver(WhatsAPIDriver):

    """\___

        Events:
        - scan_success
        - scan_fail
        - new_message

    ___/"""

    def __init__(self):

        if not os.environ["SELENIUM"]:
            raise KeyError(os.environ)

        profiledir = os.path.join(".", PROFILE_DIR)
        if not os.path.exists(profiledir):
            os.makedirs(profiledir)

        super().__init__(profile=PROFILE_DIR, client="remote",
                         command_executor=os.environ["SELENIUM"])

        if self.get_status() != WhatsAPIDriverStatus.LoggedIn:
            pass

        self.contacts = list[Contact]
        self.chats = list[Chat]

        self.subscribe_new_messages(NewMessageObserver())

        self.active_conversations = {}

        self.blocked_contacts = set()


    @staticmethod
    def broadcast(event, data: dict = None):
        logger.debug(f'broadcasting event: {event}')
        io.emit(event, data, namespace=NAMESPACE, broadcast=True)

    @track
    def dump_chats(self):
        self.broadcast('retrieving_chats')
        chats: list[Chat] = self.get_all_chats()
        for c in chats[:floor(len(chats)/4)]:
            try:
                print(c.name, type(c.name), c.id)
                if c.name == "Marti k":
                    print(c.name*100)
                    c.send_message('Brah')
                    messages: list[Message] = c.get_messages(include_me=True)
                    print([m.content for m in messages])
            except Exception as e:
                print(e)

    def handle_new_message(self, chat_id):
        chat = self.get_chat_from_id(chat_id)
        for message in chat.get_messages():
            if chat.id not in self.active_conversations:
                chat.send_message('Hoi')

        chat.send_seen()

    def continuous_read(self):
        while True:
            time.sleep(3)
            for contact in driver.get_unread(include_me=True, include_notifications=True):
                chat_id = contact.chat.id
                if chat_id not in self.active_conversations:
                    self.active_conversations[chat_id] = {'chat': contact.chat,
                                                          'messages': []}
                    for message in contact.messages:
                        pprint(message.get_js_obj())

                    # self.handle_new_contact(chat_id)
                # else:


            # print(driver.get_contacts())
            print(type(driver.get_contacts()))
            # print(driver.get_status())
            # print(driver.get_battery_level())
            # print(driver.get_my_contacts())

            while True:
                time.sleep(3)
                print("Checking for more messages, status", driver.get_status())
                for contact in driver.get_unread():
                    for message in contact.messages:
                        print(json.dumps(message.get_js_obj(), indent=4))
                        print("class", message.__class__.__name__)
                        print("message", message)
                        print("id", message.id)
                        print("type", message.type)
                        print("timestamp", message.timestamp)
                        print("chat_id", message.chat_id)
                        print("sender", message.sender)
                        profile_pic_url = message.sender.profile_pic['eurl']

                        print("sender.id", message.sender.id)
                        print("sender.safe_name", message.sender.get_safe_name())
                        if message.type == "chat":
                            print("-- Chat")
                            print("safe_content", message.safe_content)
                            print("content", message.content)
                            # contact.chat.send_message(message.safe_content)
                        elif message.type == "image" or message.type == "video":
                            print("-- Image or Video")
                            print("filename", message.filename)
                            print("size", message.size)
                            print("mime", message.mime)
                            print("caption", message.caption)
                            print("client_url", message.client_url)
                            message.save_media("./")
                        else:
                            print("-- Other")
        pass

    def dump_contacts(self):
        raise NotImplementedError
        self.broadcast('retrieving_contacts')
        contacts: list[Contact] = self.get_contacts()
        self.get_my_contacts()
        for c in contacts:
            try:
                print(c.name, c.id)
                if c.name == None:
                    pass
            except Exception as e:
                print(e)
                pass
        self.broadcast('contacts_dumped')

    def request_login(self, tries=20):
        if self.get_status() == WhatsAPIDriverStatus.LoggedIn:
            logger.debug('Already logged in')
            self.dump_chats()
            return
        while tries > 0:
            if self.update_qr_wait_for_login(timeout=20):
                self.broadcast('scan_success')
                self.save_firefox_profile(remove_old=False)
                self.dump_chats()
                return
            tries -= 1
        logger.warning("Not logged in")
        self.broadcast('scan_fail')

    def update_qr_wait_for_login(self, timeout=20) -> bool:
        filename = f'{int(time.time())}.png'
        self.get_qr(f'{PATH_TO_IMAGES}/{filename}')
        self.broadcast('new_qr', data={'filename': filename})
        try:
            self.wait_for_login(timeout=timeout)
            assert self.get_status() == WhatsAPIDriverStatus.LoggedIn
        except TimeoutException:
            logger.debug("Wait for login timed out")
        return self.get_status() == WhatsAPIDriverStatus.LoggedIn


class ConvNode:

    def __init__(self, name: str, messages: list[str],
                 parents: list, transitions: dict,
                 timeout: int):

        self.name = name
        self.messages = messages
        self.parents = parents
        self.transitions = transitions
        self.timeout = timeout

    def get_child(self, message):
        if message in self.transitions:
            return self.transitions[message]
        else:
            return None


class PowerPak(ConvNode):
    name = ''

class Welcome(ConvNode):

    name = 'Welcome'
    message = 'Hallo welkom! \n' \
              'Toets *1* als je iets wilt kopen \n ' \
              'Toets *2* als je iets wilt verkopen \n ' \
              'Toets *666* als je deze non-profit wilt helpen (love) \n ' \
              '\n' \
              'P.S.: geen gekloot aub, dit is echt en ik ben hier voor iedereen. \n' \
              'Verpest het niet voor de rest.'

    parents = []
    transitions = {'1': 'BuyMenu',
                   '2': 'SellMenu',
                   '666': 'HelpMenu'}

    timeout = 120

    def __init__(self):
        super().__init__(self.name, [self.message], self.parents, self.transitions, self.timeout)


class BuyMenu(ConvNode):

    name = 'BuyMenu'

    message = 'Toets *1* voor drinks en peuken \n ' \
              'Toets *2* voor drugs \n ' \
              'Toets *3* voor fietsen \n ' \
              'Toets *4* voor prostituees \n ' \

    transitions = {'1': 'BuyDrinksAndSigs',
                   '2': 'BuyDrugs',
                   '3': 'BuyBikes',
                   '4': 'BuySex'}

    parents = ['Welcome']

    timeout = 120

    def __init__(self):
        super().__init__(self.name, [self.message], self.parents, self.transitions, self.timeout)


class Product:
    def __init__(self, quantity, price, presentation, pay_by_cash, location_bound, category):
        self.quantity = quantity
        self.price = price
        self.presentation = presentation
        self.pay_by_cash = pay_by_cash
        self.location_bound = location_bound
        self.category = category
        assert ',' not in presentation
        assert category in {'DrinksAndSigs', 'Drugs', 'Bikes', 'Sex'}


class Seller:

    def __init__(self, items: list[Product], name, rating, minimal_amt):
        self.items = items
        self.name = name
        self.rating = rating


pakkie = Product(quantity=1, price=60, presentation='gram sos',
                 pay_by_cash=True, location_bound="ring", category='Drugs')

dmt = Product(quantity=1, price=50, presentation='gram DMT', pay_by_cash=True,
              location_bound="Ring", category='Drugs')

ket = Product(quantity=1, price=25, presentation='gram keta', pay_by_cash=True,
              location_bound="ring", category="Drugs")

demonboi = Seller(items=[pakkie, ket, dmt],
                  name="DemonBoi",
                  rating=9000,
                  minimal_amt=80)

SELLERS = [demonboi]
PRODUCTS = [pakkie, dmt, ket]


class BuyDrugs(ConvNode):

    name = 'BuyDrugs'

    message1 = 'Stel hier eerst je bestelling samen, je krijgt daarna een keuzemenu met aanbieders. \n' \
              'Type aantal en product gescheiden met komma\'s, bijvoorbeeld \n:' \
              '1 gram sos, 2 gram ket, 3 gram dmt \n' \
              'Type je producten precies zoals ze hieronder gespeld zijn.'

    message2 = ["•\n•\n•\n•\n•\n•\n•\n•\n•••••••••••••••••••••••••••••••••••••"]

    def parse_order(self, order):
        pass


class RateNote(ConvNode):

    name = 'RateNode'
    message = 'Help de system door {} te raten. \n ' \
              'Toets een getal tussen 0 (was slecht) en 9 (was extra nice)'




if __name__ == "__main__":

    from logging import LogRecord
    import uuid

    import configparser
    config = configparser.ConfigParser()
    config.read('whapper.ini')

    RUN_ID = uuid.uuid4().__str__()
    DMY_TS_STRING = time.strftime('%d%m%Y') + '_' + str(int(time.time()))
    LOGFILE_NAME = DMY_TS_STRING + '_' + RUN_ID + '.log'

    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s')

    logger = logging.getLogger()  # No name for the root logger
    logger.setLevel(logging.DEBUG)

    # === Debug to file ========================================================
    if not os.path.exists('logs'):
        os.mkdir('logs')
    if not os.path.exists('logs'):
        os.mkdir('logs')
    fh = logging.FileHandler(f'logs/{LOGFILE_NAME}')
    fh.setFormatter(formatter)
    logger.addHandler(fh)

    # === Debug to stdout ======================================================
    sh = logging.StreamHandler(sys.stdout)
    sh.setFormatter(formatter)
    logger.addHandler(sh)

    # === Failures to email if can not whatsapp ================================
    import smtplib
    import email.utils
    from email.message import EmailMessage
    from logging.handlers import SMTPHandler


    class TLSSMTPHandler(SMTPHandler):

        def __init__(self, mailhost: tuple[str, int],
                     fromaddr: str, toaddrs: list[str], subject: str,
                     credentials: tuple[str, str]):
            super().__init__(mailhost=mailhost,
                             fromaddr=fromaddr, toaddrs=toaddrs,
                             subject=subject,
                             credentials=credentials, secure=None)

            self.mailhost = mailhost
            self.fromaddr = fromaddr
            self.credentials = credentials
            self.toaddr = toaddrs
            self.subject = subject

        def getSubject(self, record: LogRecord) -> str:
            return f"{self.subject}: {record.levelno}"

        @threaded
        def emit(self, record: LogRecord):
            msg = EmailMessage()
            msg['From'] = self.fromaddr
            msg['To'] = ','.join(self.toaddr)
            msg['Subject'] = self.getSubject(record)
            msg['Date'] = email.utils.localtime()
            msg.set_content(self.format(record))
            smtp = smtplib.SMTP(self.mailhost[0], self.mailhost[1])
            smtp.ehlo()
            smtp.starttls()
            smtp.ehlo()
            smtp.login(self.credentials[0], self.credentials[1])
            smtp.send_message(msg, self.fromaddr, self.toaddr)
            smtp.quit()

    assert config.getboolean('whapper', 'debug')
    if config.getboolean('logging', 'log_to_mail'):
        smtph = TLSSMTPHandler(mailhost=("smtp.gmail.com", 587),
                               fromaddr="robertmikelsons@gmail.com",
                               toaddrs=[config.get('logging', 'addressee')],
                               subject='whapper',
                               credentials=("robertmikelsons@gmail.com", "Pn98794!"))
        smtph.setLevel(logging.ERROR)
        logger.addHandler(smtph)

    from selenium.webdriver.remote.remote_connection import LOGGER
    LOGGER.setLevel(logging.WARNING)

    logging.getLogger("urllib3").setLevel(logging.WARNING)

    driver = Driver()
    logger.debug("driving")

    @client_io.on('visitor_connected', namespace=NAMESPACE)
    def on_visitor_connected():
        logger.debug('Driver knows visitor connected')
        driver.request_login(tries=20)

    time.sleep(10000)
