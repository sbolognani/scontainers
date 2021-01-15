from flask import Flask, render_template
from flask_socketio import SocketIO

import os

app = Flask(__name__)
# app.config['SECRET_KEY'] = 'KancerBoi020'
app.debug = True
import time

NAMESPACE = '/test'

import redis
a = redis.Redis(host='redis', port=6379)
print(a.connection_pool.connection_kwargs)
a.set('wat', 'cancer')

# Redis Kafka or ZMQ
socketio = SocketIO(app, logger=True, async_mode='threading',
                    message_queue='redis://redis:6379',
                    allow_upgrades=True,
                    )


# r = redis.Redis(host='cache', port=6379, db=0)
# r.set('foo', 'bar')

"""
The lion in the jungle shows no shame, it shows no pride
It does what it needs to to stay strong and to survive, yeah
"""

# ==============================================================================


def background_thread():
    """"""
    count = 0
    while True:
        socketio.emit('cancer',
                      {'data': 'Server generated event', 'count': count},
                      namespace='/test')
        socketio.sleep(60)
        count += 1


@socketio.on('connect')
def test_connect():
    print('started background task')
    socketio.emit('after_connect',  {'data': 'Lets dance'})
    socketio.start_background_task(target=background_thread)


@socketio.on('client_connected')
def handle_client_connect_event():
    print("connected")


@socketio.on('visitor_connected', namespace='/test')
def visitor_connected():
    print('server got visitor_connected')
    socketio.emit('visitor_connected', broadcast=True, namespace=NAMESPACE)


@socketio.on('marco', namespace='/test')
def marco():
    print('polo')
    socketio.emit('polo')

@app.route('/')
def imgcube():
    return render_template('imgcube.html')


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 7777))
    socketio.run(app, host='0.0.0.0', port=port, debug=True)
    # app.run(host='0.0.0.0', port=port)