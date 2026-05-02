import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { JobOfferService } from '@core/services/job-offer.service';
import { AuthService } from '@core/services/auth.service';
import { CloudinaryService, CloudinaryFile } from '@core/services/cloudinary.service';
import { PosthogService } from '@core/services/posthog.service';
import { CountryService, Country } from '@core/services/country.service';
import { 
  CreateJobOfferDto, 
  UpdateJobOfferDto,
  JobCategory, 
  BudgetType, 
  ExperienceLevel,
  JobOfferStatus
} from '@core/models/job-offer.model';

@Component({
  selector: 'app-create-job-offer',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './create-job-offer.component.html',
  styleUrls: ['./create-job-offer.component.scss']
})
export class CreateJobOfferComponent implements OnInit {
  jobOfferForm!: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  isEditMode = false;
  jobId: string | null = null;
  
  // Enums for template
  categories = Object.values(JobCategory);
  budgetTypes = Object.values(BudgetType);
  experienceLevels = Object.values(ExperienceLevel);
  
  // Skills management
  availableSkills: string[] = [
    'Angular', 'React', 'Vue.js', 'Node.js', 'Java', 'Python', 'PHP',
    'JavaScript', 'TypeScript', 'HTML', 'CSS', 'SCSS', 'Bootstrap',
    'Figma', 'Adobe XD', 'Photoshop', 'Illustrator',
    'SEO', 'Content Writing', 'Digital Marketing', 'Social Media'
  ];
  selectedSkills: string[] = [];
  newSkillInput: string = '';
  
  // File management
  uploadedFiles: File[] = [];
  cloudinaryFiles: CloudinaryFile[] = [];
  isUploading = false;

  // Country autocomplete
  countries: Country[] = [];
  filteredCountries: Country[] = [];
  showCountrySuggestions = false;

  constructor(
    private fb: FormBuilder,
    private jobOfferService: JobOfferService,
    private authService: AuthService,
    private cloudinaryService: CloudinaryService,
    private posthogService: PosthogService,
    private countryService: CountryService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadCountries();

    // Check if we're in edit mode
    this.jobId = this.route.snapshot.paramMap.get('id');
    if (this.jobId) {
      this.isEditMode = true;
      this.posthogService.trackPageView('edit_job_offer');
      this.loadJobOffer(this.jobId);
    } else {
      this.posthogService.trackPageView('create_job_offer');
    }
  }

  loadJobOffer(id: string): void {
    this.jobOfferService.getJobOfferById(id).subscribe({
      next: (job) => {
        this.jobOfferForm.patchValue({
          title: job.title,
          category: job.category,
          description: job.description,
          budget: job.budget,
          budgetType: job.budgetType,
          estimatedDuration: job.estimatedDuration,
          deadline: job.deadline ? new Date(job.deadline).toISOString().split('T')[0] : '',
          experienceLevel: job.experienceLevel,
          location: job.location,
          isRemote: job.isRemote
        });
        this.selectedSkills = job.requiredSkills ? [...job.requiredSkills] : [];
        if (job.attachments && job.attachments.length > 0) {
          this.cloudinaryFiles = job.attachments.map((url: string) => ({
            url,
            fileName: url.split('/').pop() || 'file',
            publicId: '',
            format: '',
            size: 0,
            version: 0,
            createdAt: ''
          }));
        }
      },
      error: (error: any) => {
        console.error('Error loading job offer:', error);
        this.errorMessage = 'Error loading job offer data';
      }
    });
  }

  initForm(): void {
    this.jobOfferForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(100)]],
      category: ['', Validators.required],
      description: ['', [Validators.required, Validators.minLength(50)]],
      budget: ['', [Validators.required, Validators.min(0)]],
      budgetType: [BudgetType.FIXED, Validators.required],
      estimatedDuration: ['', [Validators.required, Validators.min(1)]],
      deadline: ['', Validators.required],
      experienceLevel: [ExperienceLevel.INTERMEDIATE, Validators.required],
      location: ['', Validators.required],
      isRemote: [false]
    });
  }

  toggleSkill(skill: string): void {
    const index = this.selectedSkills.indexOf(skill);
    if (index > -1) {
      this.selectedSkills.splice(index, 1);
    } else {
      this.selectedSkills.push(skill);
    }
  }

  isSkillSelected(skill: string): boolean {
    return this.selectedSkills.includes(skill);
  }

  addCustomSkill(): void {
    const skill = this.newSkillInput.trim();
    if (!skill) return;
    
    // Check if skill already exists (case insensitive)
    const skillExists = [...this.availableSkills, ...this.selectedSkills]
      .some(s => s.toLowerCase() === skill.toLowerCase());
    
    if (skillExists) {
      alert('This skill already exists!');
      return;
    }
    
    // Add to available skills and select it
    this.availableSkills.push(skill);
    this.selectedSkills.push(skill);
    this.newSkillInput = '';
  }

  onFileSelected(event: any): void {
    const files = event.target.files;
    if (files) {
      for (let i = 0; i < files.length; i++) {
        this.uploadedFiles.push(files[i]);
      }
      // Upload to Cloudinary immediately for preview
      this.uploadToCloudinary(Array.from(files));
    }
  }

  removeFile(index: number): void {
    this.uploadedFiles.splice(index, 1);
    this.cloudinaryFiles.splice(index, 1);
  }

  private uploadToCloudinary(files: File[]): void {
    this.isUploading = true;
    this.cloudinaryService.uploadFiles(files, 'nexlance/job-attachments').subscribe({
      next: (results) => {
        this.cloudinaryFiles.push(...results);
        this.isUploading = false;
        results.forEach(f => {
          this.posthogService.trackFileUpload('job_attachment', { fileName: f.fileName, size: f.size });
        });
      },
      error: () => {
        this.isUploading = false;
        // Files are still stored locally as fallback
      }
    });
  }

  private loadCountries(): void {
    this.countryService.getAllCountries().subscribe(countries => {
      this.countries = countries;
    });
  }

  onLocationInput(event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    if (query.length < 2) {
      this.filteredCountries = [];
      this.showCountrySuggestions = false;
      return;
    }
    this.filteredCountries = this.countries
      .filter(c => c.name.toLowerCase().includes(query.toLowerCase()))
      .slice(0, 8);
    this.showCountrySuggestions = this.filteredCountries.length > 0;
  }

  selectCountry(country: Country): void {
    this.jobOfferForm.patchValue({ location: country.name });
    this.showCountrySuggestions = false;
  }

  hideCountrySuggestions(): void {
    setTimeout(() => this.showCountrySuggestions = false, 200);
  }

  async saveDraft(): Promise<void> {
    if (this.jobOfferForm.invalid) {
      this.errorMessage = 'Please fill in all required fields';
      return;
    }

    await this.submitJobOffer(JobOfferStatus.DRAFT);
  }

  async publish(): Promise<void> {
    if (this.jobOfferForm.invalid) {
      this.errorMessage = 'Please fill in all required fields';
      this.jobOfferForm.markAllAsTouched();
      return;
    }

    if (this.selectedSkills.length === 0) {
      this.errorMessage = 'Please select at least one skill';
      return;
    }

    await this.submitJobOffer(JobOfferStatus.OPEN);
  }

  private async submitJobOffer(status: JobOfferStatus): Promise<void> {
    this.isSubmitting = true;
    this.errorMessage = '';

    try {
      // Upload files via Cloudinary if available, else fallback to backend
      let attachments: string[] = [];
      if (this.cloudinaryFiles.length > 0) {
        attachments = this.cloudinaryFiles.map(f => f.url);
      } else if (this.uploadedFiles.length > 0) {
        const uploadResult = await this.jobOfferService.uploadAttachments(this.uploadedFiles).toPromise();
        attachments = uploadResult || [];
      }

      // Get current user's ID
      const currentUser = this.authService.getCurrentUser();
      if (!currentUser) {
        this.errorMessage = 'You must be logged in to create an offer';
        this.isSubmitting = false;
        return;
      }

      const formValue = this.jobOfferForm.value;
      const jobOfferDto: CreateJobOfferDto = {
        clientId: currentUser.id,
        title: formValue.title,
        description: formValue.description,
        category: formValue.category,
        budget: formValue.budget,
        budgetType: formValue.budgetType,
        estimatedDuration: formValue.estimatedDuration,
        deadline: new Date(formValue.deadline),
        requiredSkills: this.selectedSkills,
        experienceLevel: formValue.experienceLevel,
        location: formValue.location,
        isRemote: formValue.isRemote,
        attachments: attachments,
        status: status
      };

      if (this.isEditMode && this.jobId) {
        // Update existing job offer
        const updateDto: UpdateJobOfferDto = {
          title: formValue.title,
          description: formValue.description,
          category: formValue.category,
          budget: formValue.budget,
          budgetType: formValue.budgetType,
          estimatedDuration: formValue.estimatedDuration,
          deadline: new Date(formValue.deadline),
          requiredSkills: this.selectedSkills,
          experienceLevel: formValue.experienceLevel,
          location: formValue.location,
          isRemote: formValue.isRemote,
          status: status
        };

        const result = await this.jobOfferService.updateJobOffer(this.jobId, updateDto).toPromise();

        this.posthogService.trackJobOfferEvent('updated', {
          category: updateDto.category,
          budget: updateDto.budget,
          budgetType: updateDto.budgetType,
          experienceLevel: updateDto.experienceLevel,
          status: status,
          skillsCount: this.selectedSkills.length
        });

        alert('Offer updated successfully!');
      } else {
        // Create new job offer
        const result = await this.jobOfferService.createJobOffer(jobOfferDto).toPromise();

        this.posthogService.trackJobOfferEvent('created', {
          category: jobOfferDto.category,
          budget: jobOfferDto.budget,
          budgetType: jobOfferDto.budgetType,
          experienceLevel: jobOfferDto.experienceLevel,
          status: status,
          skillsCount: this.selectedSkills.length,
          attachmentsCount: attachments.length
        });

        if (status === JobOfferStatus.DRAFT) {
          alert('Offer successfully saved as draft!');
        } else {
          alert('Offer successfully published!');
        }
      }
      
      this.router.navigate(['/frontoffice/client/my-jobs']);
    } catch (error: any) {
      console.error('Error creating job offer:', error);
      
      // Extract error message from backend response
      if (error.error) {
        if (typeof error.error === 'string') {
          this.errorMessage = error.error;
        } else if (error.error.message) {
          this.errorMessage = error.error.message;
        } else {
          this.errorMessage = JSON.stringify(error.error);
        }
      } else if (error.message) {
        this.errorMessage = error.message;
      } else {
        this.errorMessage = 'An error occurred while creating the offer';
      }
      
      alert('Error: ' + this.errorMessage);
    } finally {
      this.isSubmitting = false;
    }
  }

  cancel(): void {
    this.router.navigate(['/frontoffice/client/dashboard']);
  }
}
