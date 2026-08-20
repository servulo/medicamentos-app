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
  <p>Unidades por dose: {{s.quantityPerDose}}</p>
  <a [routerLink]="['/agendas', s.id, 'editar']">Editar</a>
  @if(s.status==='ACTIVE'){ · <button class="secondary" (click)="setStatus(s,'PAUSED')">Pausar</button>}
  @if(s.status==='PAUSED'){ · <button (click)="setStatus(s,'ACTIVE')">Reativar</button>}</article>}</div>`
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
}

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `<div class="page-title"><h1>{{editId?'Editar agenda':'Nova agenda'}}</h1><a routerLink="/agendas">Voltar</a></div>
  <form class="form-card" [formGroup]="form" (ngSubmit)="save()">
    <p class="info">Todos os horários seguem o fuso <strong>{{me.profile()?.timezone||'configurado no aplicativo'}}</strong>.</p>
    @if (editId) {
      <p><strong>Medicamento:</strong> {{medicationName}}</p>
    } @else {
      <label>Medicamento
        <select formControlName="medicationId">
          <option value="">Selecione</option>
          @for (m of meds; track m.id) {
            <option [value]="m.id">{{m.name}}</option>
          }
        </select>
      </label>
    }
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
    <div class="actions">
      <button [disabled]="form.invalid">{{editId?'Salvar alterações':'Criar agenda'}}</button>
      @if (editId) {
        <button type="button" class="secondary" (click)="remove()">Excluir</button>
      }
    </div>
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
  editId = '';
  medicationName = '';
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
    this.editId = this.route.snapshot.paramMap.get('id') || '';
    this.api.medications().subscribe((v) => {
      this.meds = v;
      if (this.editId) {
        this.loadSchedule();
      } else {
        this.form.patchValue({ medicationId: this.route.snapshot.queryParamMap.get('medicationId') || '' });
      }
    });
  }

  private loadSchedule() {
    this.api.schedule(this.editId).subscribe({
      next: (s) => {
        this.medicationName = this.meds.find((m) => m.id === s.medicationId)?.name || 'Medicamento';
        this.selectedDays = [...s.daysOfWeek].sort();
        this.selectedTimes = [...s.timesOfDay].sort();
        this.form.patchValue({
          medicationId: s.medicationId,
          quantityPerDose: s.quantityPerDose,
          durationType: s.durationType,
          maxTakenDoses: s.maxTakenDoses ?? 1
        });
        this.form.controls.medicationId.clearValidators();
        this.form.controls.medicationId.updateValueAndValidity();
      },
      error: (e) => (this.error = e.message)
    });
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
    const body = {
      daysOfWeek: this.selectedDays,
      timesOfDay: this.selectedTimes,
      durationType: v.durationType,
      maxTakenDoses: v.durationType === 'FIXED_TAKEN_DOSES' ? v.maxTakenDoses : undefined,
      quantityPerDose: Math.trunc(v.quantityPerDose)
    };
    const request = this.editId
      ? this.api.updateSchedule(this.editId, body)
      : this.api.createSchedule({ ...body, medicationId: v.medicationId });
    request.subscribe({
      next: () => void this.router.navigateByUrl('/agendas'),
      error: (e) => (this.error = e.message)
    });
  }

  remove() {
    if (!this.editId) return;
    if (!confirm('Excluir esta agenda? A agenda e todo o histórico de doses serão apagados de forma permanente e não poderão ser recuperados.')) return;
    this.api.deleteSchedule(this.editId).subscribe({
      next: () => void this.router.navigateByUrl('/agendas'),
      error: (e) => (this.error = e.message)
    });
  }
}
