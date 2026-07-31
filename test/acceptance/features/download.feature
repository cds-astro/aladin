Feature: Catalog Download

  Scenario: Downloading a catalog with valid parameters
    Given the CatalogDownloader is ready
    When I request to download "Vizier" catalog "VII/1B/catalog" at RA 0, DEC 0, radius 0
    Then the file should be successfully downloaded and not empty
