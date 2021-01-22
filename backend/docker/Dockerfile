FROM python:alpine AS backend

WORKDIR /app

RUN apk -U add pcre
RUN pip3 install --upgrade pip

RUN apk -U add mysql-client
RUN apk -U add curl
RUN apk -U add --virtual build-deps python3-dev build-base linux-headers pcre-dev musl-dev
RUN pip3 install mysql-connector-python

COPY requirements.txt /app/requirements.txt
RUN pip3 install -v -r requirements.txt

COPY . /app
