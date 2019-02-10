FROM node:11

WORKDIR /usr/app

ENV MONGO_HOST=mongo
RUN npm install yarn
COPY package*.json ./
RUN yarn
COPY . .
EXPOSE 6123
CMD [ "yarn", "start" ]