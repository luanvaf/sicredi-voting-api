package dev.luan.sicredi.voting.service.port.out;

import dev.luan.sicredi.voting.domainmodel.AssociateCpf;
import dev.luan.sicredi.voting.domainmodel.AssociateEligibility;

public interface AssociateEligibilityGateway {

  AssociateEligibility check(AssociateCpf associateCpf);
}
