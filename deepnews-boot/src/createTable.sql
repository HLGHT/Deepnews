CREATE TABLE deepnews.news (
                               id INT AUTO_INCREMENT PRIMARY KEY,
                               title VARCHAR(255),
                               url VARCHAR(255),
                               content TEXT,
                               author VARCHAR(255),
                               intro TEXT,
                               publish_time DATETIME,
                               media_name VARCHAR(255),
                               images TEXT,
                               category VARCHAR(255),
                               source VARCHAR(255),
                               create_time DATETIME,
                               update_time DATETIME
);