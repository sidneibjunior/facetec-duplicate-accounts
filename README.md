# face-tec-duplicate-accounts
This sample shows the basic steps to check for duplicate accounts using FaceTec [1-to-n-search](https://dev.facetec.com/1-to-n-search) tools.

The main logic is in `/Processors/DuplicateAccountCheckProcessor.java`.

The basic steps are:
- Call [/enrollment-3d](https://dev.facetec.com/api-guide#enrollment-3d) API to enroll the user.
  - Parameters:
    - the faceMap obtained from Android SDK as parameter
    - externalDatabaseRefID: userID
  - It will return an error if the user is already enrolled
  - To avoid calling the `/enrollment-3d` for already registered users, we should test if the user is registered using [/3d-db-get](https://dev.facetec.com/api-guide#3d-db-get)
- Call [/3d-db/enroll](https://dev.facetec.com/api-guide#3d-db-enroll) API to create the user in the 3D-DB
  - Parameters:
    - externalDatabaseRefID: userId
    - groupName
  -  Result: The API will return success even if the user is already registered in 3D-DB
- Call [/3d-db/search](https://dev.facetec.com/api-guide#3d-db-search) API to look for other accounts with faceMaps similar to the faceMap stored in database for the given userId
  - Parameters:
    - externalDatabaseRefID: userId
    - groupName
  -  Result:
    - The API will return a json with the following array, containing the accounts with similar faceMap:
```
{
  ...,
  "results": [
      {
          "identifier": "user_1",
          "matchLevel": 15
      },
      {
          "identifier": "user_2",
          "matchLevel": 15
      }
  ],
  "success": true
}
```
