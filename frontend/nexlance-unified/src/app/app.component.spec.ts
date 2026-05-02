import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

describe('AppComponent', () => {
  let component: AppComponent;
  let translateService: TranslateService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AppComponent,
        RouterTestingModule,
        TranslateModule.forRoot()
      ]
    }).compileComponents();

    translateService = TestBed.inject(TranslateService);
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    expect(component).toBeTruthy();
  });

  it(`should have the correct title`, () => {
    const fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    expect(component.title).toEqual('NexLance - Freelance Platform');
  });

  it('should set default language to English on init', () => {
    spyOn(translateService, 'setDefaultLang');
    spyOn(translateService, 'use');
    spyOn(localStorage, 'getItem').and.returnValue(null);

    const fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    component.ngOnInit();

    expect(translateService.setDefaultLang).toHaveBeenCalledWith('en');
    expect(translateService.use).toHaveBeenCalledWith('en');
  });

  it('should use saved language from localStorage', () => {
    spyOn(translateService, 'setDefaultLang');
    spyOn(translateService, 'use');
    spyOn(localStorage, 'getItem').and.returnValue('fr');

    const fixture = TestBed.createComponent(AppComponent);
    component = fixture.componentInstance;
    component.ngOnInit();

    expect(translateService.use).toHaveBeenCalledWith('fr');
  });
});
