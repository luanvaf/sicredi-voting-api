package dev.luan.sicredi.voting.service.error;

import dev.luan.sicredi.voting.domainmodel.AssociateCpf;

public final class AssociateNotFoundException extends VotingApplicationException {

  public AssociateNotFoundException(AssociateCpf associateCpf) {
    super(
        "ASSOCIATE_NOT_FOUND",
        "Associate " + associateCpf.masked() + " was not found by the eligibility provider");
  }
}
