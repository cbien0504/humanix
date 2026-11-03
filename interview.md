### 1. Tell me about yourself.

I'm Bien, a Java backend developer with about three years of experience. Im finishing a degree of Computer Science in Talented Engineer Program at Hust. Most recently, at Bee Tech Asia, I built
Humanix, a complete HRM web application with Java 17, JSP/Servlet, JDBC, jQuery and SQL Server. It covers employee
information, attendance and leave, payroll, recruitment and performance management, with role-based access for admins,
HR staff, managers and employees.

My strongest area is SQL Server: database design, stored procedures, views, triggers and performance tuning with execution
plans. I also care a lot about data quality and root-cause troubleshooting – I like to prove a fix with numbers and tests.

Before that, at Theinfitech, I worked on event-driven microservices with Spring Boot and Kafka for a customer care platform,
and on a CRM where I built Elasticsearch search over more than 20 million records.

I'm applying for this role because I want to go deeper into HRM systems, where SQL skills, business rules and close work with
HR users all come together.

### 2. You worked on microservices and Kafka before. Why do you want an IT HRM Developer role?

Two reasons. First, HR systems are rule-heavy: payroll, insurance, personal income tax, leave balances, approval workflows.
Every rule has exceptions, and a mistake directly affects people's salaries. That suits me, because my strengths are SQL and
turning business rules into precise logic, and I enjoy work where accuracy really matters.

Second, I've already built a full HRM system end to end, so I know the domain is not just CRUD screens – it's month-end payroll
deadlines, data migration from old systems and integration with time clocks. My microservices background helps with exactly
that integration side: REST APIs, bulk data, retries and tracing problems through logs.

### 3. Your CV shows university from 2021 to 2026 and work since February 2024. How did you manage both?

`[Explain honestly: whether the job was full-time or part-time, how many hours per week, and your graduation status –
e.g. "I worked full-time from my third year; my remaining courses were scheduled in the evenings / I completed my thesis in …
and graduated in …"]`

What I learned from it is to plan my week carefully and to be very clear with my team about my availability, so nobody was
ever surprised by a deadline.


---

## B. Employee lifecycle & working with HR

### 4. Walk me through the employee lifecycle in an HRM system.

It starts in recruitment: a job posting, candidates moving through screening, interview and offer. When a candidate accepts,
they become an employee on probation, and their record carries over from recruitment so HR doesn't type it twice.

After probation the employee becomes official. During employment there are changes HR must track with dates: salary raises,
transfers to another department or position, a new manager, dependants for tax. Every month they generate attendance, leave and
a payslip, and once or twice a year a performance review.

At the end, the employee is terminated with a termination date. We never delete that record, because payroll, tax and insurance
history must be kept – the employee is just marked as having left, and their login is disabled automatically.

### 5. You were the only developer on Humanix. How did you get requirements from HR without a business analyst?

I acted as my own analyst. I listed who uses the system – employees, managers, HR staff and admins – and what each of them needs
to do. Then I wrote every business rule as a numbered sentence, for example "check-in after 8:35 counts as late, and late minutes
are counted from 8:30".

When a rule was unclear, I asked with a concrete example instead of a general question, like "if someone works exactly four hours,
is that a half day?". Concrete examples get clear answers. I also wrote a permission matrix – who can see and do what – and confirmed
it with HR, because in HR software access rules matter as much as features.

### 6. How do you decide who can see what in an HR system?

Salary and personal data are the most sensitive data in a company, so the default is "need to know".

An employee sees only their own data: attendance, leave, payslips and reviews. A manager sees their team's attendance, leave requests
and reviews, but not salaries, national IDs or bank accounts – they need to approve leave, not to know pay. HR sees everyone, because
they run payroll. The admin manages accounts and system settings.

Two extra rules: nobody approves their own request, even a manager or HR, and an employee only sees a payslip after HR has approved
that payroll, so nobody sees an unfinished number. Sensitive actions like salary changes or payroll approval are recorded in an audit log.

---

## C. Payroll

### 7. Describe the monthly payroll process from start to finish.

First, the attendance cut-off: attendance and leave for the month are finalised, and managers or HR correct missing check-ins.

Then HR runs the calculation. In Humanix the period moves from "open" to "calculated", and HR can recalculate as many times as
needed while corrections come in. HR reviews the result: totals by department, unusual payslips, new joiners and leavers.

When the numbers are right, HR approves the period. From that moment it's locked and employees can see their payslips.
After the bank transfer, HR marks it as paid. The payroll sheet is exported to Excel for the bank and for accounting.

The key point is that an approved period never changes silently. If there's a mistake, it's fixed through an adjustment in the next
period, so what was paid and reported for tax always matches the history.

### 8. Explain how you calculate net salary from gross salary.

Start from the contract salary and allowances. If the employee didn't work the full month, both are prorated by paid days –
worked days plus paid leave, capped at the standard working days.

Then deduct the employee's insurance: 8% social insurance, 1.5% health insurance and 1% unemployment insurance – 10.5% in total.

For tax, taxable income is gross income minus insurance, minus the personal deduction of 15.5 million and 6.2 million for each
registered dependant. Personal income tax is progressive, so each part of the income is taxed at its own bracket rate.

Net salary is gross minus insurance minus tax. For example, 30 million gross with one dependant: insurance 3,150,000, taxable income
5,150,000, tax 265,000, so the employee receives 26,585,000.

### 9. What do you know about social insurance contributions in Vietnam?

The employee pays 10.5%: 8% social insurance, 1.5% health insurance and 1% unemployment insurance. The employer pays on top of that,
21.5% in total, so the full cost of insurance is 32% of the contribution base.

There are caps. Social and health insurance are capped at 20 times the base salary reference, which is 46.8 million at 2,340,000.
Unemployment insurance is capped at 20 times the regional minimum wage, which is 106.2 million for region I.

There's also an important monthly rule: if an employee doesn't work and isn't paid for 14 working days or more in a month,
no social insurance is paid for that month. Humanix applies that rule automatically, and all rates and caps are stored as settings,
so HR can update them when the government changes them.

### 10. How do family deductions and dependants work for personal income tax?

Every employee gets a personal deduction, now 15.5 million per month, and 6.2 million for each dependant – usually children or
parents they support. Dependants must be registered with the tax authority, so in the system the number of dependants is part of the
employee record and HR updates it when the employee submits the documents.

At the end of the year the company can do the tax finalisation for employees who authorise it. That's why the monthly payslip data –
gross income, insurance, deductions and tax withheld – must be accurate and kept. In Humanix the deductions are settings, not fixed in
the program, because they changed from 2026 and will change again.

### 11. Are all allowances treated the same for tax and insurance?

No, and that's a common source of mistakes. Some allowances are tax-exempt within a limit, for example lunch allowance. And not every
allowance is part of the insurance contribution base – it depends on whether it's a fixed allowance written in the labour contract.

In Humanix, allowance is currently one amount per employee, which is enough for the demo. In a real company I would split allowances
into types, each with two flags: "taxable" and "included in insurance base", and let HR configure them. I mention this openly because
it's the first thing I would improve before using the system for real payroll.

### 12. How do you handle employees who join or leave in the middle of the month?

Their salary and allowances are prorated by the days they were actually employed and worked. Attendance only counts days between the
hire date and the termination date, so the paid days are naturally lower.

Insurance follows the 14-day rule: if they have 14 or more unpaid working days in that month, there's no social insurance for the month.
Annual leave is prorated too: someone who joins in the middle of the year gets leave in proportion to the months they will work.

For leavers, HR also needs to settle unused leave and the final payslip, and the employee's account is disabled from the termination date.

### 13. How would you check that payroll is correct before HR approves it?

I would never approve on trust. First, compare totals with last month by department: a big change needs a reason, like new hires or a raise.
Second, look at exceptions: new joiners, leavers, employees with unpaid leave, people whose net pay changed a lot, anyone with zero or negative values.

Third, recalculate a few payslips by hand from the rules – I keep a worked example for that. In Humanix there are also automatic
data checks, for example payslips that don't match the formula or salaries outside the position's salary band, and HR can run them before approval.

### 14. An employee says their salary is wrong. What do you do?

First I listen and get specifics: which month, which amount they expected and why. Then I check the payslip line by line with them:
paid days, salary, allowance, insurance, tax.

Most of the time the cause is in the input data, not the formula: a missing check-in, leave that wasn't approved in time, a raise
entered with the wrong date, or a dependant not registered. I check the attendance and leave records and the audit log to see who changed what.

If there really is an error, the approved period isn't edited; HR corrects it with an adjustment in the next payroll, and I explain clearly
to the employee what happened and when they will receive the difference. Then I fix the cause so it doesn't happen to others.

### 15. How should salary changes and raises be handled?

A salary change always has an effective date, and the old value must be kept. In Humanix every salary change is stored in a salary
history with the old amount, the new amount, who changed it and when, so HR can always answer "why did my salary change?".

The salary also has to fit the salary band of the position; if it doesn't, the system refuses it, which prevents typing errors like an
extra zero. If a raise is approved late but applies from an earlier month, the difference is paid as an adjustment in the next payroll,
not by recalculating months that were already paid.

---

## D. Attendance & leave

### 16. What attendance rules did you implement, and why use the server time?

Work starts at 8:30 with five minutes of grace. Checking in at 8:35 is still on time; at 8:36 it's late, and late minutes count from 8:30,
so that's six minutes. Working under four hours counts as a half day.

Check-in time always comes from the server clock, not the employee's phone or computer. Otherwise anyone could change their clock and
appear on time. Each person checks in once per day, and they can't check in on a day they're on approved leave.

### 17. What happens when an employee forgets to check in or out?

That happens every day in real companies. The employee tells their manager or HR, and HR corrects the record in the attendance management
screen. The status and late minutes are recalculated automatically from the corrected times, so HR doesn't decide them by hand.

Every correction is recorded with who made it. Before payroll cut-off, HR should review missing records, because a missing check-in
means a missing paid day on the payslip.

### 18. How do you handle overtime?

Humanix doesn't calculate overtime yet, but I know the rules I would build on. Overtime must be requested and approved before or right
after it happens, so it's a workflow, not just extra hours on the clock.

Under the Labour Code, overtime is paid at least 150% on normal working days, 200% on weekly days off and 300% on public holidays, plus
extra for night work, and there are limits on overtime hours per month and per year. So the system needs an approval step, the type of day,
the rate, and a check against the monthly and yearly limits before the hours go into payroll.

### 19. How is annual leave entitlement calculated?

The standard is 12 days per year for normal working conditions, plus one extra day for every five years of service.
Someone who joins in the middle of the year gets leave in proportion to the months they work.

In Humanix, HR initialises the balances at the start of each year for everyone in one step. The available balance an employee sees is
entitlement minus days already used minus days in requests still waiting for approval. Counting pending days is important: otherwise
someone could send several requests at once and go over their balance.

### 20. What types of leave exist, and how do they affect pay?

Annual leave is paid by the company and uses the balance. Unpaid leave reduces paid days and therefore salary, and many unpaid days can
also stop insurance for that month.

Some personal leave is paid without using the balance: for example three days for your own wedding, or three days when a parent,
spouse or child dies. Sick leave and maternity leave are mostly paid by the social insurance fund, not by the company payroll, so the
system needs to track those days separately. Each leave type in Humanix has its own settings: paid or not, uses the balance or not, and a
maximum number of days per request.

### 21. Describe the leave approval workflow and its edge cases.

The employee submits a request with dates and a reason. The system counts only working days – weekends and public holidays are skipped –
checks the balance and checks that it doesn't overlap another request.

The direct manager or HR approves or rejects; a rejection must include a reason, and nobody can approve their own request. If two people
click "approve" at the same moment, only one action counts and the other gets a clear message.

The employee can cancel a pending request, or an approved one before the leave starts, and the days go back to the balance.
A request can't span two years, because balances are per year, and can't be backdated more than 30 days.

### 22. How do public holidays affect the system?

Public holidays are not working days, so they're excluded when counting leave days and when calculating standard working days for payroll.
If someone requests leave from Friday to Tuesday and Monday is a holiday, the request is two days, not five.

They also matter for overtime, because work on a public holiday is paid at the highest rate. In Humanix the holiday calendar is a table;
a screen for HR to maintain it each year is on my improvement list.

---

## E. Recruitment & performance

### 23. Describe the recruitment process you implemented.

HR creates a job posting as a draft with the department, position, number of openings and salary range, then opens it to receive candidates.
Candidates move through a pipeline shown as a Kanban board: applied, screening, interview, offer, hired. At any stage they can be rejected or withdraw,
but they can't skip stages, so every hire has gone through the process.

Managers can see candidates for their own department, rate them and add interview notes; HR manages everything. When a candidate is hired,
the system creates a probationary employee record from their data, and the posting closes automatically when all openings are filled.

### 24. What do you know about probation?

Under the Labour Code, probation can last up to 180 days for company managers, up to 60 days for jobs that need a college degree or higher,
30 days for jobs needing an intermediate qualification and six working days for other jobs. Probation salary must be at least 85% of the
official salary.

In the system, a new employee starts with status "probation". At the end, HR either converts them to "official", with the new salary if it
changes, or ends the contract. The important thing is to track the end date, so HR is reminded before probation expires.

### 25. How does the performance review process work, and how is it linked to pay?

HR creates a review cycle, for example the first half of the year, with weighted criteria such as work quality, productivity and teamwork.
The weights must add up to 100%. When the cycle opens, every eligible employee gets a review form.

The employee assesses themselves first, scoring each criterion from 1 to 5, then the manager scores. The final score is 30% self-assessment
and 70% manager score, and gives a rating from A to E.

The results feed decisions like raises, bonuses or promotion. HR also looks at the distribution of ratings by department: if one department
rates everyone A, that's a calibration problem, not a high-performing team.

---

## F. Data, reports & change

### 26. How would you migrate employee data from Excel or an old system?

I treat it as a business project, not just an import. First, agree with HR on the target format and clean the source: duplicate employee
codes, missing emails, inconsistent department names.

Then run the import in "validate only" mode. The system reports each problem by row and column, HR fixes the file and we repeat until it's clean.
Only then do we import, either all rows in one go or the valid rows first.

After the import, reconcile with HR: headcount per department, total salary fund, and a sample of records checked by hand. And keep the original
file, so we can always trace where a value came from.

### 27. What reports does HR usually need?

Headcount by department, with official, probation and long-leave employees, and the gender split. Turnover rate – leavers divided by the average
headcount – and new hires per year. Attendance: on-time rate, late arrivals and minutes, absences, per employee and per department.

For payroll: total gross and net per period and per department, and the trend over the last months, which is what management looks at.
In Humanix these are available on screen and as Excel or CSV exports, and a manager only sees the departments they are responsible for.

### 28. The law changes – for example new tax deductions. How should the system handle it?

Rates and amounts should never be written into the program. In Humanix the insurance rates, caps, reference salary, minimum wage and tax deductions
are settings, and the tax brackets are a table, so when the family deductions changed to 15.5 million and 6.2 million, it was an update by an admin, not
a new release.

The improvement I'd make next is an effective date for each setting, so a month is always calculated with the rules valid for that month. Today that's
protected because approved periods are locked, but effective dates are the proper solution.

### 29. HR asks for an urgent change two days before payroll. What do you do?

First I find out exactly what they need and why it's urgent – often it's a one-off case, like a special allowance for one employee, which can be handled
as a manual adjustment without changing the system.

If it really needs a change in the calculation, I explain the risk honestly: changing payroll logic two days before payment is dangerous. I would agree
on the smallest safe option, test it on last month's data and compare the results, and have HR check a few payslips before approval. A permanent fix can follow
after payroll, without time pressure.

### 30. HR and managers disagree on a requirement. How do you handle it?

For example, managers might want to see their team's salaries to plan budgets, while HR wants salaries to stay confidential. I don't pick a side myself.

I write down both needs and the risk of each option, and suggest alternatives – for example a report with the total salary cost of the team without individual
amounts. Then the decision goes to whoever owns that policy, usually HR or management. Once it's decided, I write it into the permission rules and the documentation,
so it's clear and consistent for everyone.

---

## G. Closing

### 31. Do you have any questions for us?

Yes, a few:

- Which HRM system do you use today – built in-house or customised – and for how many employees?
- What are the biggest problems with payroll or attendance right now that this role should solve first?
- How do developers work with the HR department – is there a business analyst, and who writes the requirements?
- How often do payroll or HR policies change, and how are those changes communicated to IT?
- What would you expect from me in the first three to six months?
