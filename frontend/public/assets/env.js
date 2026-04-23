var config = {
  "url": "http://localhost:8085",
  "oidcEnabled": true,
  "oidc": {
    "authority": "https://your-idp.example.com/realms/your-realm",
    "clientId": "llm-proxy-frontend",
    "scope": "openid profile email",
    "redirectUrl": "http://localhost:4200",
    "postLogoutRedirectUri": "http://localhost:4200",
    "silentRenew": false,
    "useRefreshToken": true
  }
};

window.AppConfig = config;
