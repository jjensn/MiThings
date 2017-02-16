FROM ruby:2.3.1
ENV RAILS_LOG_TO_STDOUT=1
RUN apt-get update -qq && apt-get install -y build-essential libpq-dev nodejs
RUN mkdir /app
WORKDIR /app
ADD Gemfile /app/Gemfile
ADD Gemfile.lock /app/Gemfile.lock
ADD . /app
RUN bundle install # --without development test
RUN RAILS_ENV=production bin/rails assets:precompile