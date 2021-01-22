import schedule
import time
from functools import wraps
from threading import Thread
from .utils import parametrized


def create_scheduler(app):
    class Scheduler(object):
        def __init__(self):
            @parametrized
            def scheduled(func, schedule_base):
                @wraps(func)
                def decorated_func(*args, **kwargs):
                    with app.app_context():
                        return func(**kwargs)
                schedule_base.do(decorated_func)
                return decorated_func

            def start_scheduler_executor():
                def run_scheduler():
                    while True:
                        schedule.run_pending()
                        time.sleep(1)

                print("Starting a new schedule executor")
                t = Thread(target=run_scheduler)
                t.start()

            self.scheduled = scheduled
            self.start_scheduler_executor = start_scheduler_executor
    return Scheduler()
