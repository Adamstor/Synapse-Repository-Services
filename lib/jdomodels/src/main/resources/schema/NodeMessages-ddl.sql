CREATE TABLE `NODE_MESSAGES` (
  `NODE_ID` bigint(20) NOT NULL,
  `THREAD_ID` bigint(20) NOT NULL,
  PRIMARY KEY (`NODE_ID`),
  UNIQUE KEY (`THREAD_ID`),
  CONSTRAINT `NODE_ID_FK` FOREIGN KEY (`NODE_ID`) REFERENCES `JDONODE` (`ID`) ON DELETE CASCADE,
  CONSTRAINT `THREAD_ID_FK` FOREIGN KEY (`THREAD_ID`) REFERENCES `MESSAGE` (`THREAD_ID`) ON DELETE CASCADE
)