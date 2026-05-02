import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatStepperModule } from '@angular/material/stepper';
import { ProjectService } from '../../../../../core/services/project.service';
import { AuthService } from '../../../../../core/services/auth.service';
import { UserService } from '../../../../../core/services/user.service';
import { CreateProjectRequest } from '../../../../../core/models/project.model';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-create-project',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatStepperModule
  ],
  templateUrl: './create-project.component.html',
  styleUrls: ['./create-project.component.scss']
})
export class CreateProjectComponent {
  projectForm: FormGroup;
  
  constructor(
    private fb: FormBuilder,
    private projectService: ProjectService,
    private authService: AuthService,
    private userService: UserService,
    private router: Router
  ) {
    const currentUser = this.authService.getCurrentUser();
    
    this.projectForm = this.fb.group({
      jobOfferId: [''],
      title: ['', Validators.required],
      freelanceEmail: ['', [Validators.required, Validators.email]],
      clientEmail: [currentUser?.email || '', [Validators.required, Validators.email]],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      requirements: [''],
      deliverables: [''],
      milestones: this.fb.array([])
    });
    
    // Disable client email field since it's auto-populated
    if (currentUser?.email) {
      this.projectForm.get('clientEmail')?.disable();
    }
    
    this.addMilestone(); // Add at least one milestone
  }

  get milestones(): FormArray {
    return this.projectForm.get('milestones') as FormArray;
  }

  createMilestoneForm(): FormGroup {
    return this.fb.group({
      title: ['', Validators.required],
      description: ['', Validators.required],
      orderIndex: [this.milestones.length + 1],
      dueDate: ['', Validators.required],
      acceptanceCriteria: [''],
      deliverables: ['']
    });
  }

  addMilestone(): void {
    this.milestones.push(this.createMilestoneForm());
  }

  removeMilestone(index: number): void {
    this.milestones.removeAt(index);
    // Reorder remaining milestones
    this.milestones.controls.forEach((control, i) => {
      control.patchValue({ orderIndex: i + 1 });
    });
  }

  moveMilestoneUp(index: number): void {
    if (index === 0) return;
    const milestone = this.milestones.at(index);
    this.milestones.removeAt(index);
    this.milestones.insert(index - 1, milestone);
    this.reorderMilestones();
  }

  moveMilestoneDown(index: number): void {
    if (index === this.milestones.length - 1) return;
    const milestone = this.milestones.at(index);
    this.milestones.removeAt(index);
    this.milestones.insert(index + 1, milestone);
    this.reorderMilestones();
  }

  private reorderMilestones(): void {
    this.milestones.controls.forEach((control, i) => {
      control.patchValue({ orderIndex: i + 1 });
    });
  }

  onSubmit(): void {
    if (this.projectForm.valid) {
      // Get raw value to include disabled fields (clientEmail)
      const formData = this.projectForm.getRawValue();
      const freelanceEmail = formData.freelanceEmail;
      const clientEmail = formData.clientEmail;
      
      // Fetch user IDs from emails
      forkJoin({
        freelancer: this.userService.getUserByEmail(freelanceEmail),
        client: this.userService.getUserByEmail(clientEmail)
      }).subscribe({
        next: ({ freelancer, client }) => {
          // Build project data with IDs instead of emails
          const projectData: CreateProjectRequest = {
            ...formData,
            freelanceId: freelancer.id,
            clientId: client.id,
            // Remove email fields as backend expects IDs
            freelanceEmail: undefined,
            clientEmail: undefined
          };
          
          // Create the project
          this.projectService.createProject(projectData).subscribe({
            next: (project) => {
              alert('Project created successfully!');
              this.router.navigate(['/frontoffice/client/projects', project.id, 'dashboard']);
            },
            error: (error) => {
              console.error('Error creating project:', error);
              alert('Error creating project');
            }
          });
        },
        error: (error) => {
          console.error('Error fetching users:', error);
          if (error.status === 404) {
            alert('User not found. Please verify the email addresses.');
          } else {
            alert('Error searching for users');
          }
        }
      });
    }
  }
}
