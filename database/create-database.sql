IF DB_ID(N'OrchidJWTLab') IS NULL
BEGIN
    CREATE DATABASE [OrchidJWTLab];
END
GO

SELECT name AS LabDatabase
FROM sys.databases
WHERE name = N'OrchidJWTLab';
GO
