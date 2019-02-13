FROM python:3.7-stretch

RUN apt-get update && apt-get install -y cron

WORKDIR /usr/app

COPY files/cron-jobs /etc/cron.d/cron-jobs
RUN chmod 0644 /etc/cron.d/cron-jobs

COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
ENV FLASK_APP=processor.py
ENV FASK_ENV=development
ENV MONGO_HOST=mongo

EXPOSE 5000

RUN crontab /etc/cron.d/cron-jobs
RUN service cron start

CMD ["python", "-m", "flask", "run", "--host=0.0.0.0"]