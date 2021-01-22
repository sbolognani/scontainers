import schedule
import time
from random import random
from flask import Flask
from flask_cors import CORS
from flask_restplus import Api
from flask_migrate import Migrate
from prometheus_flask_exporter import PrometheusMetrics
from .scheduler import create_scheduler
from .database import db as database
from .config import DATABASE_CONNECTION_URI, CLIENT_SECRETS, IS_DEBUG
from .response import make_response

app = Flask(__name__)
CORS(app)
app.config['SQLALCHEMY_DATABASE_URI'] = DATABASE_CONNECTION_URI
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
database.init_app(app)
migrate = Migrate(app, database)
scheduler = create_scheduler(app)
metrics = PrometheusMetrics(app, '/api/metrics')
metrics.info('app_info', 'Application info', version='1.0.0')


@app.before_first_request
def start_scraper():
    # This will always be called eventually because of the healthcheck on
    # the flask container
    # database.create_all()
    scheduler.start_scheduler_executor()


@scheduler.scheduled(schedule.every().friday.at("17:00"))
def slack_week_stats():
    print("It's 17:00 time to cleanup")

@app.route('/api/online/')
def isOnline():
    msg = "Hello World Development!" if IS_DEBUG else \
        "Hello World Production server!"
    return make_response(True, msg)

@app.route('/api/random/')
def getRandom():
    msg = "The random number is " + str(random())
    return make_response(True, msg)