import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

type SortDirection = 'asc' | 'desc';
type AccountCurrency = 'PLN' | 'EUR' | 'USD' | 'CHF' | 'NOK';
type OperationType = 'WPLATA' | 'WYPLATA' | 'TRANSAKCJA';
type OperationStatus = 'ZAREJESTROWANA' | 'ZREALIZOWANA' | 'ANULOWANA';
type ActiveView = 'users' | 'accounts' | 'transactions';

interface BankUser {
  id: number;
  firstName: string;
  lastName: string;
  login: string;
  birthDate: string;
  createdAt: string;
}

interface UserColumn {
  key: keyof BankUser;
  label: string;
}

interface Account {
  id: number;
  number: string;
  createdAt: string;
  currency: AccountCurrency;
  balance: number;
  lastOperationAt: string | null;
  ownerId: number;
  ownerFirstName?: string | null;
  ownerLastName?: string | null;
}

interface AccountColumn {
  key: keyof Account;
  label: string;
}

interface AccountForm {
  number: string;
  currency: AccountCurrency;
  balance: number | null;
}

interface Operation {
  id: number;
  type: OperationType;
  currency?: AccountCurrency | null;
  sourceAccountId: number | null;
  sourceAccountNumber: string | null;
  sourceAccountCurrency?: AccountCurrency | null;
  sourceOwnerId: number | null;
  sourceOwnerFirstName: string | null;
  sourceOwnerLastName: string | null;
  targetAccountId: number | null;
  targetAccountNumber: string | null;
  targetAccountCurrency?: AccountCurrency | null;
  targetOwnerId: number | null;
  targetOwnerFirstName: string | null;
  targetOwnerLastName: string | null;
  amount: number;
  operationAt: string;
  initiatedByUserId: number;
  initiatedByFirstName: string;
  initiatedByLastName: string;
  status: OperationStatus;
}

interface OperationColumn {
  key: keyof Operation;
  label: string;
}

interface PageResponse<T> {
  content: T[];
  page?: number;
  number?: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnDestroy {
  private readonly http = inject(HttpClient);
  private searchDebounceId: ReturnType<typeof setTimeout> | null = null;
  private accountSearchDebounceId: ReturnType<typeof setTimeout> | null = null;
  private transactionSearchDebounceId: ReturnType<typeof setTimeout> | null = null;
  private accountRefreshId: ReturnType<typeof setInterval> | null = null;
  private transactionRefreshId: ReturnType<typeof setInterval> | null = null;

  protected readonly users = signal<BankUser[]>([]);
  protected readonly accountOwners = signal<Record<number, BankUser>>({});
  protected readonly accounts = signal<Account[]>([]);
  protected readonly operations = signal<Operation[]>([]);
  protected readonly loading = signal(true);
  protected readonly accountsLoading = signal(false);
  protected readonly operationsLoading = signal(false);
  protected readonly accountSaving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly accountsError = signal<string | null>(null);
  protected readonly operationsError = signal<string | null>(null);
  protected readonly accountFormError = signal<string | null>(null);
  protected readonly accountFormMessage = signal<string | null>(null);
  protected readonly showTitlePage = signal(true);
  protected readonly activeView = signal<ActiveView>('users');
  protected readonly selectedUser = signal<BankUser | null>(null);
  protected readonly transactionUser = signal<BankUser | null>(null);
  protected readonly searchText = signal('');
  protected readonly accountSearchText = signal('');
  protected readonly transactionSearchText = signal('');
  protected readonly sortKey = signal<keyof BankUser>('lastName');
  protected readonly sortDirection = signal<SortDirection>('asc');
  protected readonly accountSortKey = signal<keyof Account>('id');
  protected readonly accountSortDirection = signal<SortDirection>('asc');
  protected readonly operationSortKey = signal<keyof Operation>('operationAt');
  protected readonly operationSortDirection = signal<SortDirection>('desc');
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(10);
  protected readonly totalElements = signal(0);
  protected readonly totalPages = signal(0);
  protected readonly currencies = signal<AccountCurrency[]>(['PLN']);
  protected readonly operationTypes: OperationType[] = ['WPLATA', 'WYPLATA', 'TRANSAKCJA'];
  protected readonly accountAutoRefresh = signal(true);
  protected readonly accountRefreshSeconds = signal(5);
  protected readonly transactionAutoRefresh = signal(true);
  protected readonly transactionRefreshSeconds = signal(5);
  protected readonly accountForm = signal<AccountForm>({
    number: '',
    currency: 'PLN',
    balance: 0
  });

  protected readonly columns: UserColumn[] = [
    { key: 'id', label: 'ID' },
    { key: 'firstName', label: 'Imie' },
    { key: 'lastName', label: 'Nazwisko' },
    { key: 'login', label: 'Login' },
    { key: 'birthDate', label: 'Data urodzenia' },
    { key: 'createdAt', label: 'Utworzono' }
  ];

  protected readonly accountColumns: AccountColumn[] = [
    { key: 'id', label: 'ID' },
    { key: 'number', label: 'Numer' },
    { key: 'ownerLastName', label: 'Wlasciciel' },
    { key: 'currency', label: 'Waluta' },
    { key: 'balance', label: 'Saldo' },
    { key: 'createdAt', label: 'Utworzono' },
    { key: 'lastOperationAt', label: 'Ostatnia operacja' }
  ];

  protected readonly operationColumns: OperationColumn[] = [
    { key: 'id', label: 'ID' },
    { key: 'type', label: 'Typ' },
    { key: 'sourceAccountNumber', label: 'Konto z' },
    { key: 'targetAccountNumber', label: 'Konto do' },
    { key: 'amount', label: 'Kwota' },
    { key: 'operationAt', label: 'Data' },
    { key: 'initiatedByLastName', label: 'Klient' }
  ];

  protected readonly filteredAccounts = computed(() => {
    const phrase = this.normalize(this.accountSearchText());
    const accounts = this.sortAccounts(this.accounts());

    if (!phrase) {
      return accounts;
    }

    return accounts.filter((account) => this.accountSearchValue(account).includes(phrase));
  });

  protected readonly filteredOperations = computed(() => {
    const phrase = this.normalize(this.transactionSearchText());
    const operations = this.sortOperations(this.operations());

    if (!phrase) {
      return operations;
    }

    return operations.filter((operation) => this.operationSearchText(operation).includes(phrase));
  });

  protected readonly pageLabel = computed(() => {
    if (this.totalPages() === 0) {
      return 'Strona 0 z 0';
    }

    return `Strona ${this.pageIndex() + 1} z ${this.totalPages()}`;
  });

  constructor() {
    this.loadUsers();
    this.loadAccountOwners();
    this.loadCurrencies();
    setTimeout(() => this.showTitlePage.set(false), 1000);
  }

  ngOnDestroy(): void {
    if (this.searchDebounceId) {
      clearTimeout(this.searchDebounceId);
    }
    if (this.accountSearchDebounceId) {
      clearTimeout(this.accountSearchDebounceId);
    }
    if (this.transactionSearchDebounceId) {
      clearTimeout(this.transactionSearchDebounceId);
    }
    this.stopAccountAutoRefresh();
    this.stopTransactionAutoRefresh();
  }

  protected updateSearch(value: string): void {
    this.searchText.set(value);
    this.pageIndex.set(0);

    if (this.searchDebounceId) {
      clearTimeout(this.searchDebounceId);
    }

    this.searchDebounceId = setTimeout(() => this.loadUsers(), 250);
  }

  protected sortBy(key: keyof BankUser): void {
    if (this.sortKey() === key) {
      this.sortDirection.update((direction) => direction === 'asc' ? 'desc' : 'asc');
    } else {
      this.sortKey.set(key);
      this.sortDirection.set('asc');
    }

    this.pageIndex.set(0);
    this.loadUsers();
  }

  protected sortMarker(key: keyof BankUser): string {
    if (this.sortKey() !== key) {
      return '';
    }

    return this.sortDirection() === 'asc' ? 'rosnaco' : 'malejaco';
  }

  protected sortAccountsBy(key: keyof Account): void {
    if (this.accountSortKey() === key) {
      this.accountSortDirection.update((direction) => direction === 'asc' ? 'desc' : 'asc');
    } else {
      this.accountSortKey.set(key);
      this.accountSortDirection.set('asc');
    }

    this.accounts.update((accounts) => this.sortAccounts(accounts));
  }

  protected accountSortMarker(key: keyof Account): string {
    if (this.accountSortKey() !== key) {
      return '';
    }

    return this.accountSortDirection() === 'asc' ? 'rosnaco' : 'malejaco';
  }

  protected sortOperationsBy(key: keyof Operation): void {
    if (this.operationSortKey() === key) {
      this.operationSortDirection.update((direction) => direction === 'asc' ? 'desc' : 'asc');
    } else {
      this.operationSortKey.set(key);
      this.operationSortDirection.set('asc');
    }
  }

  protected operationSortMarker(key: keyof Operation): string {
    if (this.operationSortKey() !== key) {
      return '';
    }

    return this.operationSortDirection() === 'asc' ? 'rosnaco' : 'malejaco';
  }

  protected formatCreatedAt(value: string): string {
    return value.slice(0, 10);
  }

  protected formatDate(value: string): string {
    return value ? value.slice(0, 10) : '';
  }

  protected formatOptionalDate(value: string | null): string {
    return value ? value.slice(0, 10) : '-';
  }

  protected formatOptionalDateTime(value: string | null): string {
    return value ? value.slice(0, 19).replace('T', ' ') : '-';
  }

  protected accountOwnerName(account: Account): string {
    const owner = this.findUserById(account.ownerId);
    const name = [
      account.ownerFirstName || owner?.firstName,
      account.ownerLastName || owner?.lastName
    ].filter(Boolean).join(' ');

    return name || '-';
  }

  protected updateAccountNumber(value: string): void {
    this.accountForm.update((form) => ({ ...form, number: value }));
  }

  protected updateAccountCurrency(value: AccountCurrency): void {
    this.accountForm.update((form) => ({ ...form, currency: value }));
  }

  protected updateAccountBalance(value: string): void {
    const balance = value === '' ? null : Number(value);
    this.accountForm.update((form) => ({ ...form, balance }));
  }

  protected updateAccountSearch(value: string): void {
    this.accountSearchText.set(value);
    this.clearSelectedAccountUserIfSearchChanged(value);

    if (this.accountSearchDebounceId) {
      clearTimeout(this.accountSearchDebounceId);
    }

    this.accountSearchDebounceId = setTimeout(() => {
      this.accounts.update((accounts) => this.sortAccounts(accounts));
    }, 250);
  }

  protected updateTransactionSearch(value: string): void {
    this.transactionSearchText.set(value);
    this.clearTransactionUserIfSearchChanged(value);

    if (this.transactionSearchDebounceId) {
      clearTimeout(this.transactionSearchDebounceId);
    }

    this.transactionSearchDebounceId = setTimeout(() => {
      this.operations.update((operations) => this.sortOperations(operations));
    }, 250);
  }

  protected updateAccountAutoRefresh(enabled: boolean): void {
    this.accountAutoRefresh.set(enabled);

    if (enabled) {
      this.startAccountAutoRefresh();
    } else {
      this.stopAccountAutoRefresh();
    }
  }

  protected updateAccountRefreshSeconds(value: string | number): void {
    const seconds = Math.max(1, Math.floor(Number(value) || 1));
    this.accountRefreshSeconds.set(seconds);

    if (this.accountAutoRefresh()) {
      this.startAccountAutoRefresh();
    }
  }

  protected updateTransactionAutoRefresh(enabled: boolean): void {
    this.transactionAutoRefresh.set(enabled);

    if (enabled) {
      this.startTransactionAutoRefresh();
    } else {
      this.stopTransactionAutoRefresh();
    }
  }

  protected updateTransactionRefreshSeconds(value: string | number): void {
    const seconds = Math.max(1, Math.floor(Number(value) || 1));
    this.transactionRefreshSeconds.set(seconds);

    if (this.transactionAutoRefresh()) {
      this.startTransactionAutoRefresh();
    }
  }

  protected canCreateAccount(): boolean {
    const form = this.accountForm();

    return /^\d{5}$/.test(form.number.trim())
      && this.selectedUser() !== null
      && form.balance !== null
      && !Number.isNaN(form.balance)
      && form.balance >= 0
      && !this.accountSaving();
  }

  protected previousPage(): void {
    this.goToPage(this.pageIndex() - 1);
  }

  protected nextPage(): void {
    this.goToPage(this.pageIndex() + 1);
  }

  protected goToPage(pageIndex: number): void {
    const maxPageIndex = Math.max(this.totalPages() - 1, 0);
    const nextPageIndex = Math.min(Math.max(pageIndex, 0), maxPageIndex);

    if (nextPageIndex === this.pageIndex() && !this.error()) {
      return;
    }

    this.pageIndex.set(nextPageIndex);
    this.loadUsers();
  }

  protected openAccounts(user: BankUser): void {
    this.activeView.set('accounts');
    this.transactionUser.set(null);
    this.stopTransactionAutoRefresh();
    this.selectedUser.set(user);
    this.accountSearchText.set(this.userFullName(user));
    this.accountFormError.set(null);
    this.accountFormMessage.set(null);
    this.resetAccountForm();
    this.loadAccounts();
    this.startAccountAutoRefreshIfEnabled();
  }

  protected openAllAccounts(): void {
    this.activeView.set('accounts');
    this.transactionUser.set(null);
    this.stopTransactionAutoRefresh();
    this.selectedUser.set(null);
    this.accountSearchText.set('');
    this.accountFormError.set(null);
    this.accountFormMessage.set(null);
    this.resetAccountForm();
    this.loadAccounts();
    this.startAccountAutoRefreshIfEnabled();
  }

  protected closeAccounts(): void {
    this.activeView.set('users');
    this.selectedUser.set(null);
    this.accountSearchText.set('');
    this.accounts.set([]);
    this.accountsError.set(null);
    this.accountFormError.set(null);
    this.accountFormMessage.set(null);
    this.stopAccountAutoRefresh();
  }

  protected openTransactions(user: BankUser): void {
    this.activeView.set('transactions');
    this.selectedUser.set(null);
    this.stopAccountAutoRefresh();
    this.transactionUser.set(user);
    this.transactionSearchText.set(this.userFullName(user));
    this.loadAccounts();
    this.loadOperations();
    this.startTransactionAutoRefreshIfEnabled();
  }

  protected openAllTransactions(): void {
    this.activeView.set('transactions');
    this.selectedUser.set(null);
    this.stopAccountAutoRefresh();
    this.transactionUser.set(null);
    this.transactionSearchText.set('');
    this.loadAccounts();
    this.loadOperations();
    this.startTransactionAutoRefreshIfEnabled();
  }

  protected closeTransactions(): void {
    this.activeView.set('users');
    this.transactionUser.set(null);
    this.transactionSearchText.set('');
    this.operationsError.set(null);
    this.stopTransactionAutoRefresh();
  }

  protected formatDateTime(value: string): string {
    return value ? value.slice(0, 19).replace('T', ' ') : '';
  }

  protected operationCurrency(operation: Operation): AccountCurrency | null {
    if (operation.currency) {
      return operation.currency;
    }

    if (operation.sourceAccountCurrency) {
      return operation.sourceAccountCurrency;
    }

    if (operation.targetAccountCurrency) {
      return operation.targetAccountCurrency;
    }

    const sourceCurrency = this.findAccountCurrency(operation.sourceAccountId, operation.sourceAccountNumber);

    if (sourceCurrency) {
      return sourceCurrency;
    }

    return this.findAccountCurrency(operation.targetAccountId, operation.targetAccountNumber);
  }

  protected createAccount(): void {
    const user = this.selectedUser();
    const form = this.accountForm();

    if (!user) {
      return;
    }

    if (!/^\d{5}$/.test(form.number.trim())) {
      this.accountFormError.set('Numer rachunku musi zawierac dokladnie 5 cyfr.');
      this.accountFormMessage.set(null);
      return;
    }

    if (form.balance === null || Number.isNaN(form.balance) || form.balance < 0) {
      this.accountFormError.set('Saldo poczatkowe nie moze byc ujemne.');
      this.accountFormMessage.set(null);
      return;
    }

    this.accountFormError.set(null);
    this.accountFormMessage.set(null);
    this.accountSaving.set(true);

    this.http.post<Account>('/api/accounts', {
      number: form.number.trim(),
      currency: form.currency,
      balance: form.balance,
      ownerId: user.id
    }).subscribe({
      next: () => {
        this.accountFormMessage.set('Rachunek zostal utworzony.');
        this.accountSaving.set(false);
        this.resetAccountForm();
        this.loadAccounts();
      },
      error: () => {
        this.accountFormError.set('Nie udalo sie utworzyc rachunku.');
        this.accountSaving.set(false);
      }
    });
  }

  private loadUsers(): void {
    this.loading.set(true);
    this.error.set(null);

    const params = {
      page: this.pageIndex(),
      size: this.pageSize(),
      sort: `${this.sortKey()},${this.sortDirection()}`,
      search: this.searchText().trim()
    };

    this.http.get<PageResponse<BankUser> | BankUser[]>('/api/users', { params }).subscribe({
      next: (response) => {
        this.applyUsersResponse(response);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Nie udalo sie pobrac listy uzytkownikow.');
        this.loading.set(false);
      }
    });
  }

  private applyUsersResponse(response: PageResponse<BankUser> | BankUser[]): void {
    if (Array.isArray(response)) {
      const phrase = this.normalize(this.searchText());
      const sortedUsers = response
        .filter((user) => {
          if (!phrase) {
            return true;
          }

          return this.normalize(`${user.firstName} ${user.lastName}`).includes(phrase);
        })
        .sort((left, right) => {
          const key = this.sortKey();
          return this.compare(left[key], right[key], this.sortDirection());
        });
      const start = this.pageIndex() * this.pageSize();

      this.users.set(sortedUsers.slice(start, start + this.pageSize()));
      this.mergeAccountOwners(sortedUsers);
      this.totalElements.set(sortedUsers.length);
      this.totalPages.set(Math.ceil(sortedUsers.length / this.pageSize()));
      return;
    }

    this.users.set(response.content);
    this.mergeAccountOwners(response.content);
    this.pageIndex.set(response.page ?? response.number ?? 0);
    this.pageSize.set(response.size);
    this.totalElements.set(response.totalElements);
    this.totalPages.set(response.totalPages);
  }

  private loadAccounts(ownerId?: number, showLoading = true): void {
    if (showLoading) {
      this.accountsLoading.set(true);
    }
    this.accountsError.set(null);

    const options = ownerId === undefined ? {} : { params: { ownerId } };

    this.http.get<Account[]>('/api/accounts', options).subscribe({
      next: (accounts) => {
        this.accounts.set(this.sortAccounts(accounts));
        this.accountsLoading.set(false);
      },
      error: () => {
        this.accountsError.set('Nie udalo sie pobrac rachunkow uzytkownika.');
        this.accountsLoading.set(false);
      }
    });
  }

  private loadAccountOwners(): void {
    const params = {
      page: 0,
      size: 10000,
      sort: 'lastName,asc',
      search: ''
    };

    this.http.get<PageResponse<BankUser> | BankUser[]>('/api/users', { params }).subscribe({
      next: (response) => this.mergeAccountOwners(this.usersFromResponse(response))
    });
  }

  private loadCurrencies(): void {
    this.http.get<AccountCurrency[]>('/api/accounts/currencies').subscribe({
      next: (currencies) => {
        if (currencies.length > 0) {
          this.currencies.set(currencies);
          this.accountForm.update((form) => ({ ...form, currency: currencies[0] }));
        }
      }
    });
  }

  private loadOperations(showLoading = true): void {
    if (showLoading) {
      this.operationsLoading.set(true);
    }
    this.operationsError.set(null);

    this.http.get<Operation[]>('/api/operations').subscribe({
      next: (operations) => {
        this.operations.set(this.sortOperations(operations));
        this.operationsLoading.set(false);
      },
      error: () => {
        this.operationsError.set('Nie udalo sie pobrac listy transakcji.');
        this.operationsLoading.set(false);
      }
    });
  }

  private sortAccounts(accounts: Account[]): Account[] {
    const key = this.accountSortKey();
    const direction = this.accountSortDirection();

    return [...accounts].sort((left, right) => this.compareNullable(left[key], right[key], direction));
  }

  private sortOperations(operations: Operation[]): Operation[] {
    const key = this.operationSortKey();
    const direction = this.operationSortDirection();

    return [...operations].sort((left, right) => this.compareNullable(left[key], right[key], direction));
  }

  private compare(left: string | number, right: string | number, direction: SortDirection): number {
    const modifier = direction === 'asc' ? 1 : -1;

    if (typeof left === 'number' && typeof right === 'number') {
      return (left - right) * modifier;
    }

    return String(left).localeCompare(String(right), 'pl', {
      numeric: true,
      sensitivity: 'base'
    }) * modifier;
  }

  private compareNullable(
    left: string | number | null | undefined,
    right: string | number | null | undefined,
    direction: SortDirection
  ): number {
    if ((left === null || left === undefined) && (right === null || right === undefined)) {
      return 0;
    }

    if (left === null || left === undefined) {
      return direction === 'asc' ? 1 : -1;
    }

    if (right === null || right === undefined) {
      return direction === 'asc' ? -1 : 1;
    }

    return this.compare(left, right, direction);
  }

  private normalize(value: string): string {
    return value.trim().toLocaleLowerCase('pl');
  }

  private operationSearchText(operation: Operation): string {
    return this.normalize([
      operation.sourceAccountNumber,
      operation.targetAccountNumber,
      operation.sourceOwnerFirstName,
      operation.sourceOwnerLastName,
      operation.targetOwnerFirstName,
      operation.targetOwnerLastName,
      operation.initiatedByFirstName,
      operation.initiatedByLastName
    ].filter(Boolean).join(' '));
  }

  private accountSearchValue(account: Account): string {
    const owner = this.findUserById(account.ownerId);

    return this.normalize([
      account.number,
      account.ownerFirstName,
      account.ownerLastName,
      owner?.firstName,
      owner?.lastName
    ].filter(Boolean).join(' '));
  }

  private userFullName(user: BankUser): string {
    return `${user.firstName} ${user.lastName}`;
  }

  private clearSelectedAccountUserIfSearchChanged(value: string): void {
    const user = this.selectedUser();

    if (user && this.normalize(value) !== this.normalize(this.userFullName(user))) {
      this.selectedUser.set(null);
      this.accountFormError.set(null);
      this.accountFormMessage.set(null);
    }
  }

  private clearTransactionUserIfSearchChanged(value: string): void {
    const user = this.transactionUser();

    if (user && this.normalize(value) !== this.normalize(this.userFullName(user))) {
      this.transactionUser.set(null);
    }
  }

  private findUserById(userId: number): BankUser | undefined {
    const selectedUser = this.selectedUser();
    const transactionUser = this.transactionUser();

    if (selectedUser?.id === userId) {
      return selectedUser;
    }

    if (transactionUser?.id === userId) {
      return transactionUser;
    }

    return this.accountOwners()[userId] ?? this.users().find((user) => user.id === userId);
  }

  private usersFromResponse(response: PageResponse<BankUser> | BankUser[]): BankUser[] {
    return Array.isArray(response) ? response : response.content;
  }

  private mergeAccountOwners(users: BankUser[]): void {
    if (users.length === 0) {
      return;
    }

    this.accountOwners.update((owners) => {
      const nextOwners = { ...owners };

      for (const user of users) {
        nextOwners[user.id] = user;
      }

      return nextOwners;
    });
  }

  private resetAccountForm(): void {
    this.accountForm.set({
      number: '',
      currency: 'PLN',
      balance: 0
    });
  }

  private findAccountCurrency(accountId: number | null, accountNumber: string | null): AccountCurrency | null {
    const account = this.accounts().find((item) => {
      if (accountId !== null && item.id === accountId) {
        return true;
      }

      return accountNumber !== null && item.number === accountNumber;
    });

    return account?.currency ?? null;
  }

  private startAccountAutoRefreshIfEnabled(): void {
    if (this.accountAutoRefresh()) {
      this.startAccountAutoRefresh();
    }
  }

  private startTransactionAutoRefreshIfEnabled(): void {
    if (this.transactionAutoRefresh()) {
      this.startTransactionAutoRefresh();
    }
  }

  private startAccountAutoRefresh(): void {
    this.stopAccountAutoRefresh();

    this.accountRefreshId = setInterval(() => {
      if (this.activeView() === 'accounts') {
        this.loadAccounts(undefined, false);
      }
    }, this.accountRefreshSeconds() * 1000);
  }

  private stopAccountAutoRefresh(): void {
    if (this.accountRefreshId) {
      clearInterval(this.accountRefreshId);
      this.accountRefreshId = null;
    }
  }

  private startTransactionAutoRefresh(): void {
    this.stopTransactionAutoRefresh();

    this.transactionRefreshId = setInterval(() => {
      if (this.activeView() === 'transactions') {
        this.loadOperations(false);
      }
    }, this.transactionRefreshSeconds() * 1000);
  }

  private stopTransactionAutoRefresh(): void {
    if (this.transactionRefreshId) {
      clearInterval(this.transactionRefreshId);
      this.transactionRefreshId = null;
    }
  }

}
