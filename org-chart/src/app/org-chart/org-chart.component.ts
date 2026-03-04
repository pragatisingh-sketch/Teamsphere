import { Component, OnInit, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import * as go from 'gojs'; // Import GoJS
import { Router } from '@angular/router'; // Import Router
import { UserService } from '../user.service';
import { NotificationService } from '../shared/notification.service';
import { LoaderService } from '../shared/loader.service';

@Component({
  selector: 'app-org-chart',
  templateUrl: './org-chart.component.html',
  styleUrls: ['./org-chart.component.css']
})
export class OrgChartComponent implements OnInit, AfterViewInit {

  private myDiagram: go.Diagram | undefined;
  @ViewChild('diagramDiv') diagramDiv: ElementRef | undefined;

  constructor(private router: Router , private userService:UserService,
    private notificationService:NotificationService,private loaderService: LoaderService,
  ) { }

  user: any[] = [];
  changedUser: any[] = [];
  expandedNodes: Set<number> = new Set();
  private clickTimer: any = null;
  private clickDelay = 300;

  ngOnInit(): void {}

  ngAfterViewInit(): void {
    this.initDiagram();
    this.loadDiagram();
    this.zoomToFit();
  }

  initDiagram(): void {
    const $ = go.GraphObject.make;

    this.myDiagram = $(go.Diagram, 'myDiagramDiv', {
      'undoManager.isEnabled': true,
      layout: $(go.TreeLayout, { angle: 90, layerSpacing: 40 }),
    });

this.myDiagram.nodeTemplate =
  $(go.Node, "Auto", {
    width: 150,
    height: 120,
    cursor: "pointer",
    click: (e, node) => this.handleNodeClick(e, node),
    doubleClick: (e, node) => this.onNodeDoubleClick(node)

  },
    $(go.Shape, "RoundedRectangle", {
      fill: "white",
      stroke: "#D3D3D3",
      strokeWidth: 2,
    }),
    $(go.Panel, "Vertical", { padding: 5 },
      $(go.Shape, "Circle", {
        desiredSize: new go.Size(40, 40),
        fill: null,
        strokeWidth: 0
      }),
      $(go.Picture, {
        width: 40,
        height: 40,
        margin: new go.Margin(5, 0, 5, 0),
        background: "white"
      },
        new go.Binding("source", "pic")
      ),
      // Name
      $(go.TextBlock, {
        font: "bold 12px sans-serif",
        margin: 2,
        stroke: "#0078D4",
        wrap: go.TextBlock.WrapFit,
        textAlign: "center",
        maxLines: 1,
        overflow: go.TextBlock.OverflowEllipsis,
      },
        new go.Binding("text", "name")
      ),
      // Title
      $(go.TextBlock, {
        font: "11px sans-serif",
        margin: 2,
        stroke: "#555555",
        wrap: go.TextBlock.WrapFit,
        textAlign: "center",
        maxLines: 1,
        overflow: go.TextBlock.OverflowEllipsis,
      },
        new go.Binding("text", "title")
      ),
      // Department
      $(go.TextBlock, {
        font: "10px sans-serif",
        margin: 2,
        stroke: "#888888",
        wrap: go.TextBlock.WrapFit,
        textAlign: "center",
        maxLines: 1,
        overflow: go.TextBlock.OverflowEllipsis,
      },
        new go.Binding("text", "dept")
      ),
      // Expand/Collapse Button
      $(go.Panel, "Horizontal", { alignment: go.Spot.BottomCenter, margin: new go.Margin(5, 0, 0, 0) },
        $(go.TextBlock, {
          font: "bold 14px sans-serif",
          stroke: "#888888",
          margin: 5,
          cursor: "pointer"
        }
        )
      ),

    )
  );
  }

  handleNodeClick(e: go.InputEvent, node: go.GraphObject): void {
    if (this.clickTimer) {
      clearTimeout(this.clickTimer);
      this.clickTimer = null;
    }
    this.clickTimer = setTimeout(() => {
      this.onNodeClick(node);
      this.clickTimer = null;
    }, this.clickDelay);
  }

  onNodeClick(node: go.GraphObject): void {
    console.log("Node Clicked");
    const goNode = node as go.Node;
    const userId = goNode.data.key;
    if (this.expandedNodes.has(userId)) {
      this.collapseNode(userId);
      this.expandedNodes.delete(userId);
    } else {
      this.expandNode(userId);
      this.expandedNodes.add(userId);
    }
  }

  onNodeDoubleClick(node: go.GraphObject): void {
    console.log("Double click called")
    if (this.clickTimer) {
      clearTimeout(this.clickTimer);
      this.clickTimer = null;
    }
    const goNode = node as go.Node;
    const userId = goNode.data.key;
    this.router.navigate(['/user-details', userId]);
  }


  loadDiagram(): void {
    if (this.myDiagram) {
        this.userService.getAllUsers().subscribe({
            next: (response) => {
                if (response.data.length === 0) {
                    const noDataFallback = {
                        "class": "go.TreeModel",
                        "nodeDataArray": [
                            { "key": 1, "name": "No data available",
                              "title": "Please try again later",
                              "dept": "",
                              "parent": 1 ,
                              "pic":`https://ui-avatars.com/api/?name=N+A&background=random`
                            }
                        ]
                    };
                    this.myDiagram!.model = go.Model.fromJson(noDataFallback);
                } else {
                  this.user = response.data;
                  this.changedUser = this.user.filter((node: { parent: number; }) => node.parent === 1);
                  const modelData = this.createModelData(this.changedUser);
                  this.myDiagram!.model = go.Model.fromJson(modelData);
                }
            },
            error: (error) => {
                const errorMessage = error.error?.message || 'Failed to load user data. Please try again.';
                this.notificationService.showError(errorMessage);
                console.error('Error fetching employee data:', error);
                const fallbackData = {
                    "class": "go.TreeModel",
                    "nodeDataArray": [
                        { "key": 1, "name": "Error", "title": "Retry", "dept": "", "parent": 1 }
                    ]
                };
                this.myDiagram!.model = go.Model.fromJson(fallbackData);
            }
        });
    } else {
        console.error('myDiagram is undefined');
    }
}

  expandNode(nodeId: any) {
    const children = this.user.filter((node: { parent: any; }) => node.parent === nodeId);
    if (children.length === 0) {
      this.notificationService.showNotification({
        type: 'error',
        message: 'No subordinates found. Try another role.',
      });
      return;
    }
    this.changedUser = [...this.changedUser, ...children];
    const updatedModelData=this.createModelData(this.changedUser);
    this.myDiagram!.model=go.Model.fromJson(updatedModelData);

  }

  collapseNode(nodeId: number): void {
    const findDescendants = (parentId: number): number[] => {
      const directChildren = this.user.filter(node => node.parent === parentId);
      const allDescendants = directChildren.flatMap(child => findDescendants(child.id));
      return [...directChildren.map(child => child.id), ...allDescendants];
    };
    const descendants = findDescendants(nodeId);
    if(descendants.length===0)
    {
      this.notificationService.showNotification({
        type: 'error',
        message: 'No subordinates found. Try another role.',
      });
      return;
    }
    this.changedUser = this.changedUser.filter(node => !descendants.includes(node.id));
    const updatedModelData = this.createModelData(this.changedUser);
    this.myDiagram!.model = go.Model.fromJson(updatedModelData);

    console.log(`Collapsed Node: ${nodeId}`, { descendants });
  }


  createModelData(employees:any)
  {
    const modelData = {
      "class": "go.TreeModel",
      "nodeDataArray": employees.map((employee: { id: any; firstName: string | number | boolean; lastName: string | number | boolean; level: any; team: any; email: any; ldap: any; profilePic: string; parent: any; }) => ({
          "key": employee.id,
          "name": `${employee.firstName} ${employee.lastName}`,
          "title": employee.level,
          "dept": employee.team,
          "email": employee.email,
          "ldap": employee.ldap,
          "pic": (!employee.profilePic || employee.profilePic === "NA" || employee.profilePic === "undefined")
              ? `https://ui-avatars.com/api/?name=${encodeURIComponent(employee.firstName)}+${encodeURIComponent(employee.lastName)}&background=random`
              : `data:image/jpeg;base64,${employee.profilePic}`,
          "phone": employee.email,
          "parent": employee.parent
      }))
  };
  return modelData;
  }

  save(): void {
    const savedModel = this.myDiagram?.model.toJson();
    console.log(savedModel);
  }

  zoomToFit(): void {
    this.myDiagram?.commandHandler.zoomToFit();
  }

  centerRoot(): void {
    const rootNode = this.myDiagram?.findNodeForKey(1);
    if (rootNode) {
      this.myDiagram?.commandHandler.scrollToPart(rootNode);
    }
  }
}
