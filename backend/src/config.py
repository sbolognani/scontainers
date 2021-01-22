import os
import json

IS_DEBUG = (int(os.getenv('FLASK_DEBUG', 0)) == 1)

EXPECTED_EMAIL_SUFFIXES = ["@something.com"]

DATE_FORMAT = "%Y-%m-%d"

user = os.getenv('MYSQL_USER')
password = os.getenv('MYSQL_PASSWORD')
database = os.getenv('MYSQL_DATABASE')
host = os.getenv('MYSQL_HOST')
DATABASE_CONNECTION_URI =\
    f'mysql+mysqlconnector://{user}:{password}@{host}/{database}?charset=utf8mb4'

with open("client_secrets.json", "r") as config_file:
    CLIENT_SECRETS = json.load(config_file)
