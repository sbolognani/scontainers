## Setup
Add the following line to your /etc/hosts file, and 
replace the ip address with the address of your docker installation.
> 192.168.99.100  somewhere.local

## Running the development environment
To run the development environment simply execute the following command.
> docker-compose -f docker-compose.yml -f dev.yml up --remove-orphans --build

This will start all of the services in debug modes which allow for hot swapping.
A refresh will be triggered by changing a file in the front or backend code.

## Running the production environment
To run in production run the following command.
> docker-compose -f docker-compose.yml -f prod.yml up --remove-orphans --build -d

This will start an nginx instance which governs access to the frontend and backend. <br />

### Updating the client_secrets.json
In production this file will be copied from the parent folder of the application.
The application itself resides in /home/app, 
and the client_secrets.json and database are found in /home.

## Inspecting the database
Go to http://somewhere.local:8080/ <br>
Server  : database<br/>
Username: pass<br/>
Password: user<br/>
Database: db<br/>
Now press test connection and it should say that a successfull connection has been made.

## Update the database
The project makes use of the flask migration library which can be found
[here](https://flask-migrate.readthedocs.io/en/latest/).<br />
Follow the steps below to migrate the database 
> \> docker ps <br />
Find the CONTAINER ID of the backend container
> \> docker exec -it <container id> sh
> \> flask db migrate
> \> flask db upgrade


## Notes
The development environment will probably not be reachable by ip. To solve this you have to add the host to the '/etc/hosts' file and flush your hosts cache.
