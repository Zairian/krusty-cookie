set foreign_key_checks = 0;

DROP TABLE IF EXISTS Customers;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS PalletCounter;
DROP TABLE IF EXISTS Pallets;
DROP TABLE IF EXISTS Cookies;
DROP TABLE IF EXISTS Ingredients;
DROP TABLE IF EXISTS Recipes;

CREATE TABLE Customers
(customerName varchar(20),
address varchar(20),
PRIMARY KEY (customerName)
);


CREATE TABLE Orders 
(orderNumber INT,
palletLabel INT,
orderDate DATE,
customer varchar(20),
PRIMARY KEY (orderNumber),
FOREIGN KEY (customer) references Customers (customerName),
FOREIGN KEY (palletLabel) references Pallets (palletLabel)
);


CREATE TABLE Pallets
(palletLabel INT auto_increment,
cookie varchar(20),
curLoc varchar(30),
packingDate datetime,
deliveryDate datetime,
blocked boolean,
PRIMARY KEY (palletLabel),
FOREIGN KEY (cookie) references Cookies (cookieName)
);


CREATE TABLE PalletCounter
(numOfPallets INT,
cookie varchar(20),
FOREIGN KEY (cookie) references Cookies (cookieName)
);


CREATE TABLE Cookies
(cookieName varchar(20),
PRIMARY KEY (cookieName)
);


CREATE TABLE Ingredients
(ingType  varchar(60),
amount INT,
unit varchar(10),
delDate DATE,
delAmount INT,
PRIMARY KEY (ingType)
);


CREATE TABLE Recipes
(cookie varchar(20),
ingType varchar(30),
amount INT,
FOREIGN KEY (cookie) references Cookies (cookieName),
FOREIGN KEY (ingType) references Ingredients (ingType)
);

set foreign_key_checks = 1;
