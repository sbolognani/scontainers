#!/bin/sh

# Wait until MySQL is ready
chmod +x docker/wait.sh
docker/wait.sh database:3306

# upgrade the database to the latest version
flask db upgrade

# start the flask server
if [ $FLASK_DEBUG -eq "1" ]; then
    flask run --port=5000 --host=0.0.0.0
else
    echo "Running production server"
    uwsgi uwsgi.ini
fi
