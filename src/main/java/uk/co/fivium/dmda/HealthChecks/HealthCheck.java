package uk.co.fivium.dmda.HealthChecks;

import fi.iki.elonen.NanoHTTPD;

public interface HealthCheck {

  NanoHTTPD.Response check();

  boolean isSecurityTokenRequired();

}
