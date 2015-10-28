package org.sagebionetworks.repo.web.service;

import java.util.List;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.verification.VerificationPagedResults;
import org.sagebionetworks.repo.model.verification.VerificationState;
import org.sagebionetworks.repo.model.verification.VerificationStateEnum;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;

public interface VerificationService {
	
	VerificationSubmission createVerificationSubmission(Long userId, VerificationSubmission verificationSubmission);
	
	VerificationPagedResults listVerificationSubmissions(
			Long userId, List<VerificationStateEnum> currentVerificationState, Long verifiedUserId, long limit, long offset);
	
	void changeSubmissionState(Long userId, long verificationSubmissionId, VerificationState newState);
	
	String getDownloadURL(Long userId, long verificationSubmissionId, long fileHandleId);

}
