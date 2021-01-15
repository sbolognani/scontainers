FROM python:3.9

WORKDIR /app
COPY requirements.txt /app

# pip install --keep-image-small --dont-bother-with-ssl -r requirements.txt 
RUN pip install --no-cache-dir --trusted-host pypi.python.org -r requirements.txt
