# ReWear Database Project 👕🌍

## 📌 Om projektet
ReWear är en fiktiv webshop som säljer second hand- och upcycle-kläder.  
Databasen är byggd för att hålla reda på:
- Produkter, varianter (storlek, färg, pris, lager)
- Varumärken
- Kategorier
- Kunder och deras ordrar
- Orderrader (kopplar ordrar till produkter)

## 📊 Innehåll i repot
- **sql/**
  - `rewear_ddl.sql` → Skapar databasen + testdata  
  - `rewear_dml.sql` → SQL-frågor (G & VG)  
- **java/**
  - `ReWearQueries.java` → JDBC-program som kör SQL-frågorna  
- **docs/**
  - `ReWear_ER_Diagram.pdf` → Tekniskt ER-diagram (3NF)  
  - `ReWear_ER_Diagram.png` → Samma som bild  
  - `ReWear_Visual_ER.pdf` → Konceptuell visuell modell för presentation  
  - `ReWear_Relationsmodell.xlsx` → Relationsmodell  
- **design/**
  - `ReWear_logo.png` → Logotyp i rosa/blå stil  

## 🚀 Så kör du projektet
1. Importera `rewear_ddl.sql` i MySQL Workbench (skapar databasen).  
2. Kör `rewear_dml.sql` i Workbench för att testa frågorna.  
3. Kompilera och kör Java-koden:  
   ```bash
   javac -cp .;mysql-connector-j-9.4.0.jar ReWearQueries.java
   java -cp .;mysql-connector-j-9.4.0.jar;. ReWearQueries
