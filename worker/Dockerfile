FROM python:3.7-stretch

RUN apt-get update && apt-get install -y cron

WORKDIR /usr/app

COPY files/cron-weekly /etc/cron.weekly/cron-weekly
RUN chmod 755 /etc/cron.weekly/cron-weekly
COPY files/crontab /etc/cron.d/cron-live
RUN chmod 0644 /etc/cron.d/cron-live

COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
ENV FLASK_APP=server.py
ENV FASK_ENV=development
ENV MONGO_HOST=mongo

EXPOSE 5000

RUN crontab /etc/cron.d/cron-live
RUN service cron start

COPY ./entrypoint.sh /
RUN chmod 755 /entrypoint.sh

ENTRYPOINT [ "/entrypoint.sh" ]
CMD ["python", "-m", "flask", "run", "--host=0.0.0.0"]