import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { UserService } from '../user.service';
import { DateFormatService } from '../services/date-format.service';

@Component({
  selector: 'app-user-details',
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.css']
})
export class UserDetailsComponent implements OnInit {

  user: any = {};

  constructor(
    private route: ActivatedRoute,
    private userService: UserService,
    private dateFormatService: DateFormatService
  ) {}

  ngOnInit(): void {
    const userId = this.route.snapshot.paramMap.get('id');
    const isInactive = this.route.snapshot.queryParamMap.get('isInactive') === 'true';

    if (userId) {
      this.userService.getUserById(userId, isInactive).subscribe(
        (response) => {
          this.user = {
            ...response.data,
            startDate: this.dateFormatService.formatDate(response.data.startDate),
            lwdMlStartDate: this.dateFormatService.formatDate(response.data.lwdMlStartDate),
            resignationDate: this.dateFormatService.formatDate(response.data.resignationDate),
            roleChangeEffectiveDate: this.dateFormatService.formatDate(response.data.roleChangeEffectiveDate),
            billingStartDate: this.dateFormatService.formatDate(response.data.billingStartDate),
            lastBillingDate: this.dateFormatService.formatDate(response.data.lastBillingDate),
            tenureTillDate: response.data.tenureTillDate
          };
        },
        (error) => {
          console.error('Error fetching user data:', error);
        }
      );
    }
  }
}
