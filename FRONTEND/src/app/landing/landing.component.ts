import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  template: `
    <div class="landing-page">
      <div class="landing-card">
        <div style="font-size:56px;margin-bottom:16px;">☁️</div>
        <h1>Cloud Cost Optimizer</h1>
        <p>Monitor, analyze, and reduce your cloud infrastructure costs with intelligent insights.</p>
        <div class="landing-actions">
          <button class="btn-primary" (click)="router.navigate(['/login'])">Sign In</button>
          <button class="btn-secondary" (click)="router.navigate(['/register'])">Create Account</button>
        </div>
        <div class="landing-features">
          <div class="feature">Real-time cost analytics</div>
          <div class="feature">Leakage detection</div>
          <div class="feature">Smart recommendations</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .landing-page {
      min-height: 100vh;
      background: linear-gradient(135deg, #1a1a1a 0%, #2a2a2a 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 20px;
    }
    .landing-card {
      background: #ffffff;
      border-radius: 24px;
      padding: 56px 48px;
      text-align: center;
      max-width: 480px;
      width: 100%;
      box-shadow: 0 40px 80px rgba(0,0,0,0.3);
    }
    h1 { font-size: 28px; font-weight: 650; color: #1a1a1a; margin-bottom: 12px; letter-spacing: -0.5px; }
    p { font-size: 14px; color: #666; margin-bottom: 36px; line-height: 1.6; }
    .landing-actions { display: flex; gap: 12px; justify-content: center; margin-bottom: 36px; }
    .btn-primary, .btn-secondary {
      padding: 12px 32px; font-size: 14px; font-weight: 550;
      border-radius: 100px; cursor: pointer; transition: all 0.2s;
    }
    .btn-primary { background: #1a1a1a; color: #fff; border: 1.5px solid #1a1a1a; }
    .btn-primary:hover { background: #333; }
    .btn-secondary { background: transparent; color: #1a1a1a; border: 1.5px solid #ccc; }
    .btn-secondary:hover { background: #f5f5f0; }
    .landing-features { display: flex; flex-direction: column; gap: 10px; }
    .feature { font-size: 13px; color: #888; padding: 8px 16px; background: #f5f5f0; border-radius: 8px; }
  `]
})
export class LandingComponent {
  constructor(public router: Router) {}
}
