package uk.co.fivium.dmda.healthchecks;

import fi.iki.elonen.NanoHTTPD;

public interface HealthCheck {

  NanoHTTPD.Response check();

  boolean isSecurityTokenRequired();

}
