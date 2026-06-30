import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  user = { username: '', password: '', email: '', role: 'PASSENGER' };
  error = '';
  success = '';

  constructor(private authService: AuthService, private router: Router) { }

  register(): void {
    this.error = '';
    this.success = '';
    this.authService.register(this.user).subscribe({
      next: (res: any) => {
        if (res && res.token) {
          localStorage.setItem('token', res.token);
          localStorage.setItem('user', JSON.stringify({ username: res.username, role: res.role }));
        }
        this.success = 'Registration successful! Redirecting to home...';
        setTimeout(() => this.router.navigate(['/']), 1500);
      },
      error: (err) => {
        console.error('Registration failed', err);
        this.error = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }
}
