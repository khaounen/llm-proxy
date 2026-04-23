import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { firstValueFrom } from 'rxjs';

type LoginMode = 'basic' | 'oidc';
type RouteAuthType = 'NONE' | 'BEARER';
type TokenExpiryPolicy = 'NEVER' | 'P30D' | 'P365D';
type ApiTokenStatus = 'ACTIVE' | 'EXPIRED' | 'REVOKED';
type AppView = 'dashboard' | 'routes' | 'tokens' | 'metrics' | 'sessions' | 'prompts' | 'tops';
type TopPeriod = 'daily' | 'monthly';
type TopDimension = 'consumers' | 'routes';

interface AuthMeResponse {
  username: string;
  issuer: string;
  subject: string;
  authenticationType: string;
  roles: string[];
}

interface CountByLabel {
  label: string;
  total: number;
}

interface ActivityPeriod {
  periodStart: string;
  periodEnd: string;
  totalRequests: number;
  uniqueUsers: number;
  totalSessions: number;
  errorRequests: number;
  errorRate: number;
  avgLatencyMs: number;
  topConsumers: CountByLabel[];
  topRoutes: CountByLabel[];
}

interface AdminDashboard {
  generatedAt: string;
  daily: ActivityPeriod;
  monthly: ActivityPeriod;
}

interface RouteConfig {
  id: number;
  name: string;
  incomingPrefix: string;
  targetBaseUrl: string;
  outboundAuthType: RouteAuthType;
  outboundBearerToken: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

interface AvailableProxyRoute {
  name: string;
  incomingPrefix: string;
}

interface ProxyMetric {
  id: number;
  sessionId: number | null;
  username: string;
  routeName: string;
  incomingPath: string;
  targetUrl: string;
  method: string;
  statusCode: number;
  latencyMs: number;
  promptPreview: string | null;
  errorMessage: string | null;
  createdAt: string;
}

interface ProxySession {
  id: number;
  username: string;
  startedAt: string;
  lastPromptAt: string;
  promptCount: number;
}

interface UserApiToken {
  id: number;
  label: string | null;
  tokenPrefix: string;
  expiryPolicy: TokenExpiryPolicy;
  expiresAt: string | null;
  revokedAt: string | null;
  lastUsedAt: string | null;
  createdAt: string;
  status: ApiTokenStatus;
}

interface UserApiTokenCreateResponse {
  plainToken: string;
  token: UserApiToken;
}

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

interface PagedResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

interface PaginationState<T> extends PagedResponse<T> {
  loading: boolean;
}

declare global {
  interface Window {
    AppConfig?: RuntimeAppConfig;
  }
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.new.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  private readonly dashboardTopMaxRows = 5;
  private readonly dashboardRecentMaxRows = 8;

  apiBaseUrl = this.resolveApiBaseUrl();
  oidcLoginEnabled = this.resolveOidcEnabled();
  private readonly oidcSecurityService = inject(OidcSecurityService, { optional: true });

  loginMode: LoginMode = 'basic';
  basicUsername = 'admin';
  basicPassword = 'admin123';

  authProfile: AuthMeResponse | null = null;
  private authHeader: string | null = null;
  activeView: AppView = 'dashboard';

  dashboard: AdminDashboard | null = null;
  recentMetrics: ProxyMetric[] = [];
  recentSessions: ProxySession[] = [];
  routes: RouteConfig[] = [];
  availableProxyRoutes: AvailableProxyRoute[] = [];
  userTokens: UserApiToken[] = [];

  metricPage = this.createPageState<ProxyMetric>(20);
  sessionPage = this.createPageState<ProxySession>(20);
  promptPage = this.createPageState<ProxyMetric>(20);
  topPage = this.createPageState<CountByLabel>(20);
  topPeriod: TopPeriod = 'daily';
  topDimension: TopDimension = 'consumers';

  loadingDashboard = false;
  loadingRoutes = false;
  loadingTokens = false;
  loadingRecentMetrics = false;
  loadingRecentSessions = false;
  creatingRoute = false;
  creatingToken = false;

  routeForm = {
    name: '',
    incomingPrefix: '/app1/llm',
    targetBaseUrl: 'https://my-llm-api',
    outboundAuthType: 'BEARER' as RouteAuthType,
    outboundBearerToken: '',
    active: true
  };

  tokenForm = {
    label: '',
    expiryPolicy: 'NEVER' as TokenExpiryPolicy
  };

  lastGeneratedToken = '';
  feedbackMessage = '';
  feedbackError = false;

  get isAuthenticated(): boolean {
    return this.authProfile !== null;
  }

  get isAdmin(): boolean {
    return this.authProfile?.roles.includes('ROLE_ADMIN') ?? false;
  }

  get canManageTokens(): boolean {
    if (!this.authProfile) {
      return false;
    }
    return this.isAdmin || this.authProfile.roles.includes('ROLE_USER');
  }

  get currentRoleLabel(): string {
    if (!this.authProfile) {
      return '';
    }
    if (this.isAdmin) {
      return 'ADMIN';
    }
    return 'USER';
  }

  get dashboardDailyConsumers(): CountByLabel[] {
    return this.dashboard?.daily.topConsumers.slice(0, this.dashboardTopMaxRows) ?? [];
  }

  get dashboardDailyRoutes(): CountByLabel[] {
    return this.dashboard?.daily.topRoutes.slice(0, this.dashboardTopMaxRows) ?? [];
  }

  get dashboardMonthlyConsumers(): CountByLabel[] {
    return this.dashboard?.monthly.topConsumers.slice(0, this.dashboardTopMaxRows) ?? [];
  }

  get dashboardMonthlyRoutes(): CountByLabel[] {
    return this.dashboard?.monthly.topRoutes.slice(0, this.dashboardTopMaxRows) ?? [];
  }

  get dashboardTrafficRows(): ProxyMetric[] {
    return this.recentMetrics.slice(0, this.dashboardRecentMaxRows);
  }

  get dashboardSessionRows(): ProxySession[] {
    return this.recentSessions.slice(0, this.dashboardRecentMaxRows);
  }

  get topPageTitle(): string {
    const periodLabel = this.topPeriod === 'daily' ? 'Daily' : 'Monthly';
    const dimensionLabel = this.topDimension === 'consumers' ? 'Top consumers' : 'Top routes';
    return `${periodLabel} - ${dimensionLabel}`;
  }

  async ngOnInit(): Promise<void> {
    if (!this.oidcLoginEnabled || !this.oidcSecurityService) {
      return;
    }

    try {
      const authResult = await firstValueFrom(this.oidcSecurityService.checkAuth());
      if (!authResult.isAuthenticated || !authResult.accessToken) {
        return;
      }

      const header = `Bearer ${authResult.accessToken}`;
      await this.completeLogin(header, true);
    } catch (error) {
      this.setMessage(this.readError(error), true);
    }
  }

  setLoginMode(mode: LoginMode): void {
    this.loginMode = mode;
    this.clearFeedback();
  }

  async loginWithBasic(): Promise<void> {
    this.clearFeedback();
    const header = `Basic ${btoa(`${this.basicUsername}:${this.basicPassword}`)}`;
    await this.completeLogin(header, false);
  }

  async loginWithOidc(): Promise<void> {
    this.clearFeedback();
    if (!this.oidcLoginEnabled || !this.oidcSecurityService) {
      this.setMessage('OIDC n\'est pas configure dans assets/env.js', true);
      return;
    }
    this.oidcSecurityService.authorize();
  }

  logout(): void {
    this.authProfile = null;
    this.authHeader = null;
    this.activeView = 'dashboard';
    this.dashboard = null;
    this.recentMetrics = [];
    this.recentSessions = [];
    this.routes = [];
    this.availableProxyRoutes = [];
    this.userTokens = [];
    this.metricPage = this.createPageState<ProxyMetric>(20);
    this.sessionPage = this.createPageState<ProxySession>(20);
    this.promptPage = this.createPageState<ProxyMetric>(20);
    this.topPage = this.createPageState<CountByLabel>(20);
    this.lastGeneratedToken = '';
    this.setMessage('Session fermee', false);
  }

  async refreshCurrentView(): Promise<void> {
    if (!this.isAuthenticated) {
      return;
    }

    if (this.activeView === 'dashboard' && this.isAdmin) {
      await Promise.all([
        this.loadDashboard(),
        this.loadRecentMetrics(this.dashboardRecentMaxRows),
        this.loadRecentSessions(this.dashboardRecentMaxRows)
      ]);
      return;
    }

    if (this.activeView === 'routes' && this.isAdmin) {
      await this.loadRoutes();
      return;
    }

    if (this.activeView === 'metrics' && this.isAdmin) {
      await this.loadMetricPage(this.metricPage.page);
      return;
    }

    if (this.activeView === 'sessions' && this.isAdmin) {
      await this.loadSessionPage(this.sessionPage.page);
      return;
    }

    if (this.activeView === 'prompts' && this.isAdmin) {
      await this.loadPromptPage(this.promptPage.page);
      return;
    }

    if (this.activeView === 'tops' && this.isAdmin) {
      await this.loadTopPage(this.topPage.page);
      return;
    }

    if (this.activeView === 'tokens' && this.canManageTokens) {
      await Promise.all([this.loadTokens(), this.loadAvailableProxyRoutes()]);
    }
  }

  async loadDashboard(): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.loadingDashboard = true;
    try {
      this.dashboard = await this.request<AdminDashboard>('/api/admin/metrics/dashboard');
    } catch (error) {
      this.setMessage(this.readError(error), true);
    } finally {
      this.loadingDashboard = false;
    }
  }

  async loadRecentMetrics(limit = 50): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.loadingRecentMetrics = true;
    try {
      this.recentMetrics = await this.request<ProxyMetric[]>(`/api/admin/metrics/recent?limit=${limit}`);
    } catch (error) {
      this.setMessage(this.readError(error), true);
    } finally {
      this.loadingRecentMetrics = false;
    }
  }

  async loadRecentSessions(limit = 50): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.loadingRecentSessions = true;
    try {
      this.recentSessions = await this.request<ProxySession[]>(`/api/admin/metrics/sessions?limit=${limit}`);
    } catch (error) {
      this.setMessage(this.readError(error), true);
    } finally {
      this.loadingRecentSessions = false;
    }
  }

  async loadMetricPage(page: number): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.metricPage.loading = true;
    try {
      const result = await this.request<PagedResponse<ProxyMetric>>(
        `/api/admin/metrics/recent/page?page=${Math.max(0, page)}&size=${this.metricPage.size}`
      );
      this.metricPage = { ...result, loading: false };
    } catch (error) {
      this.metricPage.loading = false;
      this.setMessage(this.readError(error), true);
    }
  }

  async loadSessionPage(page: number): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.sessionPage.loading = true;
    try {
      const result = await this.request<PagedResponse<ProxySession>>(
        `/api/admin/metrics/sessions/page?page=${Math.max(0, page)}&size=${this.sessionPage.size}`
      );
      this.sessionPage = { ...result, loading: false };
    } catch (error) {
      this.sessionPage.loading = false;
      this.setMessage(this.readError(error), true);
    }
  }

  async loadPromptPage(page: number): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.promptPage.loading = true;
    try {
      const result = await this.request<PagedResponse<ProxyMetric>>(
        `/api/admin/metrics/prompts/page?page=${Math.max(0, page)}&size=${this.promptPage.size}`
      );
      this.promptPage = { ...result, loading: false };
    } catch (error) {
      this.promptPage.loading = false;
      this.setMessage(this.readError(error), true);
    }
  }

  async loadTopPage(page: number): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.topPage.loading = true;
    try {
      const result = await this.request<PagedResponse<CountByLabel>>(
        `/api/admin/metrics/top/page?period=${this.topPeriod}&dimension=${this.topDimension}&page=${Math.max(0, page)}&size=${this.topPage.size}`
      );
      this.topPage = { ...result, loading: false };
    } catch (error) {
      this.topPage.loading = false;
      this.setMessage(this.readError(error), true);
    }
  }

  goToMetricPage(page: number): void {
    void this.loadMetricPage(page);
  }

  goToSessionPage(page: number): void {
    void this.loadSessionPage(page);
  }

  goToPromptPage(page: number): void {
    void this.loadPromptPage(page);
  }

  goToTopPage(page: number): void {
    void this.loadTopPage(page);
  }

  openTopView(period: TopPeriod, dimension: TopDimension): void {
    this.topPeriod = period;
    this.topDimension = dimension;
    this.switchView('tops');
  }

  onTopPeriodChange(event: Event): void {
    const select = event.target as HTMLSelectElement | null;
    if (!select) {
      return;
    }
    this.topPeriod = select.value === 'monthly' ? 'monthly' : 'daily';
    void this.loadTopPage(0);
  }

  onTopDimensionChange(event: Event): void {
    const select = event.target as HTMLSelectElement | null;
    if (!select) {
      return;
    }
    this.topDimension = select.value === 'routes' ? 'routes' : 'consumers';
    void this.loadTopPage(0);
  }

  async loadRoutes(): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.loadingRoutes = true;
    try {
      this.routes = await this.request<RouteConfig[]>('/api/admin/routes');
    } catch (error) {
      this.setMessage(this.readError(error), true);
    } finally {
      this.loadingRoutes = false;
    }
  }

  async createRoute(): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    this.creatingRoute = true;
    try {
      await this.request('/api/admin/routes', {
        method: 'POST',
        body: JSON.stringify(this.routeForm)
      });
      this.setMessage('Route creee', false);
      await this.loadRoutes();
    } catch (error) {
      this.setMessage(this.readError(error), true);
    } finally {
      this.creatingRoute = false;
    }
  }

  async toggleRoute(route: RouteConfig): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    try {
      await this.request(`/api/admin/routes/${route.id}`, {
        method: 'PUT',
        body: JSON.stringify({
          name: route.name,
          incomingPrefix: route.incomingPrefix,
          targetBaseUrl: route.targetBaseUrl,
          outboundAuthType: route.outboundAuthType,
          outboundBearerToken: route.outboundBearerToken ?? '',
          active: !route.active
        })
      });
      this.setMessage(`Route ${route.name} mise a jour`, false);
      await this.loadRoutes();
    } catch (error) {
      this.setMessage(this.readError(error), true);
    }
  }

  async deleteRoute(id: number): Promise<void> {
    if (!this.isAdmin) {
      return;
    }
    try {
      await this.request(`/api/admin/routes/${id}`, {
        method: 'DELETE'
      });
      this.setMessage(`Route ${id} supprimee`, false);
      await this.loadRoutes();
    } catch (error) {
      this.setMessage(this.readError(error), true);
    }
  }

  async loadTokens(): Promise<void> {
    if (!this.canManageTokens) {
      return;
    }
    this.loadingTokens = true;
    try {
      this.userTokens = await this.request<UserApiToken[]>('/api/me/tokens');
    } catch (error) {
      this.setMessage(this.readError(error), true);
    } finally {
      this.loadingTokens = false;
    }
  }

  async loadAvailableProxyRoutes(): Promise<void> {
    if (!this.canManageTokens) {
      return;
    }
    try {
      this.availableProxyRoutes = await this.request<AvailableProxyRoute[]>('/api/me/routes');
    } catch (error) {
      this.setMessage(this.readError(error), true);
    }
  }

  async createToken(): Promise<void> {
    if (!this.canManageTokens) {
      return;
    }
    this.creatingToken = true;
    try {
      const response = await this.request<UserApiTokenCreateResponse>('/api/me/tokens', {
        method: 'POST',
        body: JSON.stringify(this.tokenForm)
      });
      this.lastGeneratedToken = response.plainToken;
      this.setMessage('Token genere. Copie-le maintenant, il ne sera plus affiche.', false);
      await this.loadTokens();
    } catch (error) {
      this.setMessage(this.readError(error), true);
    } finally {
      this.creatingToken = false;
    }
  }

  async copyLastGeneratedToken(): Promise<void> {
    if (this.lastGeneratedToken.trim() === '') {
      return;
    }

    try {
      await navigator.clipboard.writeText(this.lastGeneratedToken);
      this.setMessage('Token copie dans le clipboard', false);
    } catch {
      this.setMessage('Impossible de copier le token', true);
    }
  }

  buildCurlExample(route: AvailableProxyRoute): string {
    const baseUrl = this.apiBaseUrl.replace(/\/+$/, '');
    const prefix = route.incomingPrefix.startsWith('/') ? route.incomingPrefix : `/${route.incomingPrefix}`;
    const token = this.lastGeneratedToken.trim() === '' ? '<YOUR_PROXY_TOKEN>' : this.lastGeneratedToken.trim();
    const targetUrl = `${baseUrl}${prefix}/v1/chat/completions`;
    return [
      `curl -X POST '${targetUrl}' \\`,
      `  -H 'Authorization: Bearer ${token}' \\`,
      "  -H 'Content-Type: application/json' \\",
      "  -d '{\"model\":\"gpt-4o-mini\",\"messages\":[{\"role\":\"user\",\"content\":\"Hello\"}]}'"
    ].join('\n');
  }

  async copyRouteCurlExample(route: AvailableProxyRoute): Promise<void> {
    try {
      await navigator.clipboard.writeText(this.buildCurlExample(route));
      this.setMessage(`Commande curl copiee pour ${route.name}`, false);
    } catch {
      this.setMessage('Impossible de copier la commande curl', true);
    }
  }

  async revokeToken(token: UserApiToken): Promise<void> {
    if (!this.canManageTokens) {
      return;
    }
    try {
      await this.request(`/api/me/tokens/${token.id}`, {
        method: 'DELETE'
      });
      this.setMessage(`Token ${token.tokenPrefix} revoque`, false);
      await this.loadTokens();
    } catch (error) {
      this.setMessage(this.readError(error), true);
    }
  }

  onTokenExpiryChange(token: UserApiToken, event: Event): void {
    if (!this.canManageTokens) {
      return;
    }
    const select = event.target as HTMLSelectElement | null;
    if (!select) {
      return;
    }
    const policy = select.value as TokenExpiryPolicy;
    void this.updateTokenExpiry(token.id, policy);
  }

  switchView(view: AppView): void {
    this.activeView = view;
    void this.refreshCurrentView();
  }

  private createPageState<T>(size: number): PaginationState<T> {
    return {
      items: [],
      page: 0,
      size,
      totalItems: 0,
      totalPages: 0,
      hasNext: false,
      hasPrevious: false,
      loading: false
    };
  }

  private async updateTokenExpiry(tokenId: number, expiryPolicy: TokenExpiryPolicy): Promise<void> {
    try {
      await this.request(`/api/me/tokens/${tokenId}/expiry`, {
        method: 'PATCH',
        body: JSON.stringify({ expiryPolicy })
      });
      this.setMessage('Expiration du token mise a jour', false);
      await this.loadTokens();
    } catch (error) {
      this.setMessage(this.readError(error), true);
    }
  }

  private async completeLogin(header: string, isOidc: boolean): Promise<void> {
    try {
      const profile = await this.request<AuthMeResponse>('/api/auth/me', {}, header);
      this.authProfile = profile;
      this.authHeader = header;

      if (profile.roles.includes('ROLE_ADMIN')) {
        this.activeView = 'dashboard';
      } else if (profile.roles.includes('ROLE_USER')) {
        this.activeView = 'tokens';
      } else {
        this.activeView = 'tokens';
      }

      await this.refreshCurrentView();
      const modeLabel = isOidc ? 'OIDC' : 'BASIC';
      this.setMessage(`Connecte en ${modeLabel} en tant que ${profile.username}`, false);
    } catch (error) {
      this.authProfile = null;
      this.authHeader = null;
      this.setMessage(this.readError(error), true);
    }
  }

  private async request<T = unknown>(path: string, init: RequestInit = {}, overrideAuthHeader?: string): Promise<T> {
    const headers = new Headers({
      Accept: 'application/json'
    });

    if (init.body && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }

    const authHeader = overrideAuthHeader ?? this.authHeader;
    if (authHeader) {
      headers.set('Authorization', authHeader);
    }

    if (init.headers) {
      new Headers(init.headers).forEach((value, key) => headers.set(key, value));
    }

    const response = await fetch(`${this.apiBaseUrl}${path}`, {
      ...init,
      headers
    });

    if (!response.ok) {
      const text = await response.text();
      throw new Error(`${response.status} ${response.statusText}: ${text}`);
    }

    if (response.status === 204) {
      return undefined as T;
    }

    const contentType = response.headers.get('content-type') ?? '';
    if (!contentType.includes('application/json')) {
      return undefined as T;
    }

    return (await response.json()) as T;
  }

  private setMessage(message: string, error: boolean): void {
    this.feedbackMessage = message;
    this.feedbackError = error;
  }

  private clearFeedback(): void {
    this.feedbackMessage = '';
    this.feedbackError = false;
  }

  private readError(error: unknown): string {
    if (error instanceof Error) {
      return error.message;
    }
    return 'Erreur inattendue';
  }

  private resolveApiBaseUrl(): string {
    const runtimeConfig = window.AppConfig;
    const runtimeUrl = runtimeConfig?.apiBaseUrl ?? runtimeConfig?.url;
    if (runtimeUrl && runtimeUrl.trim() !== '') {
      return runtimeUrl.trim();
    }
    return 'http://localhost:8085';
  }

  private resolveOidcEnabled(): boolean {
    const runtimeConfig = window.AppConfig;
    if (runtimeConfig?.oidcEnabled === false) {
      return false;
    }
    return !!runtimeConfig?.oidc?.authority && !!runtimeConfig.oidc.clientId;
  }
}
