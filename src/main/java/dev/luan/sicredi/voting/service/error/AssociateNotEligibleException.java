package dev.luan.sicredi.voting.service.error;

import dev.luan.sicredi.voting.domainmodel.AssociateCpf;

public final class AssociateNotEligibleException extends VotingApplicationException {

  public AssociateNotEligibleException(AssociateCpf associateCpf) {
    super(
        "ASSOCIATE_NOT_ELIGIBLE",
        "Associate " + associateCpf.masked() + " is not eligible to vote");
  }
}
