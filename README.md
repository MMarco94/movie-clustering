# Movie clustering

This repository is a collection of scripts that can cluster movies according to the users how saw them.

## Data source
The source of the data is an online service accessible via https://everyfad.com/movies or the Android app [MoviesFad](https://play.google.com/store/apps/details?id=fema.moviesfad).

In order to run code in this project, you need to have access to this database and to some libraries that facilitate the reading of the data. 
At the moment, those resources are not public. 

## Clustering methods
Three different clustering techniques have been used: XMeans, spectral clustering and dominant sets.  
All these methods use only on a subset of all the data: the 10000 most watched movies, and the 10000 users that watched more movies. 

PS: for illustration purposes, some images in this report may use less than 10000 items. For those images, the amount of movies and users taken into account is explicitly mentioned.

### Data representation
The same data can be viewed in two different ways: 
 - A `user` * `movie` matrix that encodes how many time a user watch a movie
 - A `movie` * `movie` matrix that encodes the similarity between two movies. For this project, I choose to compute this by using the cosine similarity between rows on the previous matrix
 
### Preliminary analysis
![User * movie matrix](out.resources/user_movie_200.png)  
This image represents the user (on rows) vs. movie (on columns) matrix on the top 200 movies and users.  
The brighter a cell, the more times a user watched that movie.  

![Movie similarity matrix](out.resources/movie_similarities_200.png)  
This image represents the movie similarity matrix of the top 200 movies (using data from the top 10000 users).  
The brighter a cell, the more times a user watched that movie.

