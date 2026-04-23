import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { OpenIdConfiguration, provideAuth } from 'angular-auth-oidc-client';

import { routes } from './app.routes';

interface RuntimeOidcConfig {
  authority?: string;
  clientId?: string;
  scope?: string;
  redirectUrl?: string;
  postLogoutRedirectUri?: string;
  silentRenew?: boolean;
  useRefreshToken?: boolean;
}

interface RuntimeAppConfig {
  url?: string;
  apiBaseUrl?: string;
  oidcEnabled?: boolean;
  oidc?: RuntimeOidcConfig;
}

declare global {
  interface Window {
    AppConfig?: RuntimeAppConfig;
  }
}

function resolveOidcConfiguration(): OpenIdConfiguration | null {
  const runtimeConfig = window.AppConfig;
  if (runtimeConfig?.oidcEnabled === false) {
    return null;
  }

  const oidc = runtimeConfig?.oidc;
  if (!oidc?.authority || !oidc.clientId) {
    return null;
  }

  return {
    authority: oidc.authority.trim(),
    clientId: oidc.clientId.trim(),
    responseType: 'code',
    scope: oidc.scope?.trim() || 'openid profile email',
    redirectUrl: oidc.redirectUrl?.trim() || window.location.origin,
    postLogoutRedirectUri: oidc.postLogoutRedirectUri?.trim() || window.location.origin,
    silentRenew: oidc.silentRenew ?? false,
    useRefreshToken: oidc.useRefreshToken ?? false
  };
}

const oidcConfig = resolveOidcConfiguration();

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideHttpClient(),
    provideRouter(routes),
    ...(oidcConfig ? [provideAuth({ config: oidcConfig })] : [])
  ]
};
