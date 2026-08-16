import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService, MeService, Medication, Schedule } from '../../core/api/api';

const dayNames = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];

const HOURLY_SLOTS: string[] = Array.from({ length: 24 }, (_, hour) =>
  `${String(hour).padStart(2, '0')}:00`
);

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `<div class="page-title"><div><h1>Agendas</h1><p>Horários no fuso {{me.profile()?.timezone||'do aplicativo'}}.</p></div><a class="button" routerLink="/agendas/nova">Nova agenda</a></div>
  <div class="grid">@for(s of items;track s.id){<article><div class="row"><h2>{{name(s.medicationId)}}</h2><span class="badge">{{status(s.status)}}</span></div>
  <p>{{days(s.daysOfWeek)}} às {{s.timesOfDay.join(', ')}}</p>
  <p>{{s.durationType==='INDEFINITE'?'Uso contínuo':s.takenCount+' de '+s.maxTakenDoses+' doses tomadas'}}</p>
  <label class="inline">Unidades por dose
    <input type="number" min="1" step="1" [value]="s.quantityPerDose" (change)="saveQuantity(s, $event)">
  </label>
  @if(s.status==='ACTIVE'){<button class="secondary" (click)="setStatus(s,'PAUSED')">Pausar</button>}
  @if(s.status==='PAUSED'){<button (click)="setStatus(s,'ACTIVE')">Reativar</button>}</article>}</div>`
})
export class SchedulesPage implements OnInit {
  readonly api = inject(ApiService);
  readonly me = inject(MeService);
  items: Schedule[] = [];
  meds: Medication[] = [];
  ngOnInit() {
    this.api.schedules().subscribe((v) => (this.items = v));
    this.api.medications().subscribe((v) => (this.meds = v));
  }
  name(id: string) {
    return this.meds.find((m) => m.id === id)?.name || 'Medicamento';
  }
  days(v: number[]) {
    return v.map((d) => dayNames[d - 1]).join(', ');
  }
  status(v: Schedule['status']) {
    return ({ ACTIVE: 'Ativa', PAUSED: 'Pausada', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' } as const)[v];
  }
  setStatus(s: Schedule, status: 'ACTIVE' | 'PAUSED') {
    this.api.updateSchedule(s.id, { status, resetTakenCount: false }).subscribe((v) => Object.assign(s, v));
  }
  saveQuantity(s: Schedule, event: Event) {
    const raw = Math.trunc(Number((event.target as HTMLInputElement).value));
    if (!Number.isFinite(raw) || raw < 1) {
      (event.target as HTMLInputElement).value = String(s.quantityPerDose);
      return;
    }
    this.api.updateSchedule(s.id, { quantityPerDose: raw }).subscribe({
      next: (v) => Object.assign(s, v),
      error: () => {
        (event.target as HTMLInputElement).value = String(s.quantityPerDose);
      }
    });
  }
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `<div class="page-title"><h1>Nova agenda</h1><a routerLink="/agendas">Voltar</a></div>
  <form class="form-card" [formGroup]="form" (ngSubmit)="save()">
    <p class="info">Todos os horários seguem o fuso <strong>{{me.profile()?.timezone||'configurado no aplicativo'}}</strong>.</p>
    <label>Medicamento
      <select formControlName="medicationId">
        <option value="">Selecione</option>
        @for (m of meds; track m.id) {
          <option [value]="m.id">{{m.name}}</option>
        }
      </select>
    </label>
    <label>Unidades por dose
      <input type="number" min="1" step="1" formControlName="quantityPerDose">
    </label>
    <fieldset>
      <legend>Dias da semana</legend>
      <div class="actions">
        <button type="button" class="secondary compact" (click)="selectAllDays()">Selecionar todos os dias</button>
      </div>
      <div class="checks">
        @for (day of dayNames; track $index) {
          <label><input type="checkbox" [checked]="selectedDays.includes($index+1)" (change)="toggleDay($index+1)"> {{day}}</label>
        }
      </div>
      @if (daysError) {
        <p class="error">{{daysError}}</p>
      }
    </fieldset>
    <div class="stack">
      <label>Adicionar horário
        <select [value]="timePickerValue" (change)="onTimePick($event)">
          <option value="">Selecione um horário</option>
          @for (slot of hourlySlots; track slot) {
            <option [value]="slot" [disabled]="selectedTimes.includes(slot)">{{slot}}</option>
          }
        </select>
      </label>
      @if (selectedTimes.length) {
        <ul class="selected-times">
          @for (t of selectedTimes; track t) {
            <li>
              <span>{{t}}</span>
              <button type="button" class="secondary compact" (click)="removeTime(t)">Remover</button>
            </li>
          }
        </ul>
      } @else {
        <p class="hint">Nenhum horário adicionado ainda.</p>
      }
      @if (timesError) {
        <p class="error">{{timesError}}</p>
      }
    </div>
    <label>Duração
      <select formControlName="durationType">
        <option value="INDEFINITE">Uso contínuo</option>
        <option value="FIXED_TAKEN_DOSES">Número de doses tomadas</option>
      </select>
    </label>
    @if (form.controls.durationType.value==='FIXED_TAKEN_DOSES') {
      <label>Total de doses<input type="number" min="1" formControlName="maxTakenDoses"></label>
    }
    @if (error) {
      <p class="error">{{error}}</p>
    }
    <button [disabled]="form.invalid">Criar agenda</button>
  </form>`
})
export class ScheduleFormPage implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly me = inject(MeService);
  readonly dayNames = dayNames;
  readonly hourlySlots = HOURLY_SLOTS;
  meds: Medication[] = [];
  selectedDays: number[] = [];
  selectedTimes: string[] = [];
  timePickerValue = '';
  daysError = '';
  timesError = '';
  error = '';
  readonly form = this.fb.nonNullable.group({
    medicationId: ['', Validators.required],
    quantityPerDose: [1, [Validators.required, Validators.min(1)]],
    durationType: ['INDEFINITE' as Schedule['durationType']],
    maxTakenDoses: [1]
  });

  ngOnInit() {
    this.api.medications().subscribe((v) => (this.meds = v));
    this.form.patchValue({ medicationId: this.route.snapshot.queryParamMap.get('medicationId') || '' });
  }

  toggleDay(day: number) {
    this.selectedDays = this.selectedDays.includes(day)
      ? this.selectedDays.filter((v) => v !== day)
      : [...this.selectedDays, day].sort();
    if (this.selectedDays.length) {
      this.daysError = '';
    }
  }

  selectAllDays() {
    this.selectedDays = [1, 2, 3, 4, 5, 6, 7];
    this.daysError = '';
  }

  onTimePick(event: Event) {
    const select = event.target as HTMLSelectElement;
    this.addTime(select.value);
    select.value = '';
    this.timePickerValue = '';
  }

  addTime(time: string) {
    if (!time || this.selectedTimes.includes(time)) {
      return;
    }
    this.selectedTimes = [...this.selectedTimes, time].sort();
    this.timesError = '';
  }

  removeTime(time: string) {
    this.selectedTimes = this.selectedTimes.filter((t) => t !== time);
  }

  save() {
    this.error = '';
    this.daysError = '';
    this.timesError = '';
    if (!this.selectedDays.length) {
      this.daysError = 'Selecione ao menos um dia da semana.';
    }
    if (!this.selectedTimes.length) {
      this.timesError = 'Escolha ao menos um horário na lista.';
    }
    if (this.form.invalid || this.daysError || this.timesError) {
      return;
    }
    const v = this.form.getRawValue();
    this.api
      .createSchedule({
        medicationId: v.medicationId,
        daysOfWeek: this.selectedDays,
        timesOfDay: this.selectedTimes,
        durationType: v.durationType,
        maxTakenDoses: v.durationType === 'FIXED_TAKEN_DOSES' ? v.maxTakenDoses : undefined,
        quantityPerDose: Math.trunc(v.quantityPerDose)
      })
      .subscribe({
        next: () => void this.router.navigateByUrl('/agendas'),
        error: (e) => (this.error = e.message)
      });
  }
}
